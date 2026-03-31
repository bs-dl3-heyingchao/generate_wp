package com.neusoft.bsdl.wptool.core.cache.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.neusoft.bsdl.wptool.core.cache.CacheEntry;
import com.neusoft.bsdl.wptool.core.cache.CacheStoreMode;
import com.neusoft.bsdl.wptool.core.cache.ShardedCache;

/**
 * 高性能分片持久化缓存 (支持异步写入 + 定时同步)
 */
public class FlexibleShardedCache implements ShardedCache {

    private static final Logger logger = LoggerFactory.getLogger(FlexibleShardedCache.class);

    private final File baseDir;
    private final int shardCount;
    private final Map<String, CacheEntry> memoryMap;
    private final ScheduledExecutorService scheduler;
    private final long defaultTtlMillis;

    private static final long INFINITE_TTL_MILLIS = Long.MAX_VALUE;

    public FlexibleShardedCache(File baseDir) {
        this(baseDir, INFINITE_TTL_MILLIS, 16);
    }

    public FlexibleShardedCache(File baseDir, int defaultTtlMinutes, int shardCount) {
        this(baseDir, TimeUnit.MINUTES.toMillis(defaultTtlMinutes), shardCount);
    }

    public FlexibleShardedCache(File baseDir, long defaultTtlMillis, int shardCount) {
        this.baseDir = baseDir;
        this.shardCount = shardCount;
        this.defaultTtlMillis = normalizeTtlMillis(defaultTtlMillis);
        this.memoryMap = new ConcurrentHashMap<>();

        if (!baseDir.exists() && !baseDir.mkdirs()) {
            logger.warn("Failed to create cache directory: {}", baseDir);
        }

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        loadAllShards();
        // 定时同步
        this.scheduler.scheduleAtFixedRate(this::flushAllShards, 5, 5, TimeUnit.MINUTES);
        logger.info("FlexibleShardedCache initialized. BaseDir: {}, Shards: {}, DefaultTTL(ms): {}", baseDir, shardCount, this.defaultTtlMillis);
    }

    @Override
    public <T> T get(String key) {
        return getInternal(key, null, false);
    }

    @Override
    public <T> T get(String key, String latestTag) {
        if (latestTag == null) {
            return getInternal(key, null, false);
        }
        return getInternal(key, latestTag, true);
    }

    private <T> T getInternal(String key, String latestTag, boolean checkTag) {
        CacheEntry entry = memoryMap.get(key);
        if (entry == null)
            return null;
        if (entry.isExpired()) {
            memoryMap.remove(key);
            flushShardAsync(key);
            return null;
        }
        if (checkTag && !latestTag.equals(entry.getTag())) {
            return null;
        }
        @SuppressWarnings("unchecked")
        T result = (T) entry.getValue();
        return result;
    }

    @Override
    public <T> void put(String key, T value) {
        putInternal(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS, null);
    }

    @Override
    public <T> void put(String key, T value, String cacheTag) {
        putInternal(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS, cacheTag);
    }

    @Override
    public <T> void put(String key, T value, CacheStoreMode mode) {
        // 当前实现暂未拆分多后端，先保持旧行为兼容。
        putInternal(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS, null);
    }

    @Override
    public <T> void put(String key, T value, CacheStoreMode mode, String cacheTag) {
        // 当前实现暂未拆分多后端，先保持旧行为兼容。
        putInternal(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS, cacheTag);
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit) {
        putInternal(key, value, ttl, timeUnit, null);
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit, String cacheTag) {
        putInternal(key, value, ttl, timeUnit, cacheTag);
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit, CacheStoreMode mode) {
        // 当前实现暂未拆分多后端，先保持旧行为兼容。
        putInternal(key, value, ttl, timeUnit, null);
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit, CacheStoreMode mode, String cacheTag) {
        // 当前实现暂未拆分多后端，先保持旧行为兼容。
        putInternal(key, value, ttl, timeUnit, cacheTag);
    }

    private <T> void putInternal(String key, T value, long ttl, TimeUnit timeUnit, String cacheTag) {
        if (value == null) {
            remove(key);
            return;
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit must not be null");
        }
        long expireTime = calculateExpireTime(ttl, timeUnit);
        memoryMap.put(key, new CacheEntry(value, expireTime, cacheTag));
        flushShardAsync(key);
    }

    @Override
    public void remove(String key) {
        if (memoryMap.remove(key) != null) {
            flushShardAsync(key);
        }
    }

    private int getShardIndex(String key) {
        return Math.abs(key.hashCode()) % shardCount;
    }

    private String getShardFilePath(int index) {
        return baseDir + File.separator + "cache_" + index + ".dat";
    }

    private void flushShardAsync(String key) {
        int index = getShardIndex(key);
        // 使用 CompletableFuture 异步执行，不阻塞主线程
        CompletableFuture.runAsync(() -> flushShard(index));
    }

    private void flushShard(int shardIndex) {
        long start = System.currentTimeMillis();
        String filePath = getShardFilePath(shardIndex);
        try {
            Map<String, CacheEntry> shardMap = new HashMap<>();
            for (Map.Entry<String, CacheEntry> entry : memoryMap.entrySet()) {
                if (getShardIndex(entry.getKey()) == shardIndex && !entry.getValue().isExpired()) {
                    shardMap.put(entry.getKey(), entry.getValue());
                }
            }
            if (shardMap.isEmpty())
                return;

            String tempPath = filePath + ".tmp";
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempPath))) {
                oos.writeObject(shardMap);
            }

            File tempFile = new File(tempPath);
            File targetFile = new File(filePath);
            if (targetFile.exists())
                targetFile.delete();
            if (!tempFile.renameTo(targetFile)) {
                logger.error("Failed to rename temp file: {} -> {}", tempPath, filePath);
            }
            logger.debug("[Shard {}] Saved {} items in {}ms", shardIndex, shardMap.size(), System.currentTimeMillis() - start);
        } catch (IOException e) {
            logger.error("[Shard {}] Failed to save: {}", shardIndex, e.getMessage(), e);
        }
    }

    private void flushAllShards() {
        for (int i = 0; i < shardCount; i++)
            flushShard(i);
    }

    private void loadAllShards() {
        logger.info("Loading shards from {}...", baseDir);
        int totalLoaded = 0;
        for (int i = 0; i < shardCount; i++) {
            String filePath = getShardFilePath(i);
            File file = new File(filePath);
            if (!file.exists())
                continue;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                @SuppressWarnings("unchecked")
                Map<String, CacheEntry> loadedMap = (Map<String, CacheEntry>) ois.readObject();
                if (loadedMap != null) {
                    for (Map.Entry<String, CacheEntry> entry : loadedMap.entrySet()) {
                        if (!entry.getValue().isExpired()) {
                            memoryMap.put(entry.getKey(), entry.getValue());
                            totalLoaded++;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Failed to load Shard {}: {}. Deleting.", i, e.getMessage());
                file.delete();
            }
        }
        logger.info("Total loaded: {} items.", totalLoaded);
    }

    @Override
    public void close() {
        logger.info("Shutting down FlexibleShardedCache...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            flushAllShards(); // 关闭前强制刷盘
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("FlexibleShardedCache closed.");
    }

    private long normalizeTtlMillis(long ttlMillis) {
        if (ttlMillis >= INFINITE_TTL_MILLIS) {
            return INFINITE_TTL_MILLIS;
        }
        return ttlMillis;
    }

    private long calculateExpireTime(long ttl, TimeUnit timeUnit) {
        if (ttl == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        long ttlMillis = timeUnit.toMillis(ttl);
        if (ttlMillis >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        long now = System.currentTimeMillis();
        if (ttlMillis > 0 && now > Long.MAX_VALUE - ttlMillis) {
            return Long.MAX_VALUE;
        }
        return now + ttlMillis;
    }
}
