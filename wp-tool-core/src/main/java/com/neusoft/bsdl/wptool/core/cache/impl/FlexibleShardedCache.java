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

import com.neusoft.bsdl.wptool.core.cache.CacheStoreMode;
import com.neusoft.bsdl.wptool.core.cache.ShardedCache;
import com.neusoft.bsdl.wptool.core.cache.entry.CacheEntry;

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

    public FlexibleShardedCache(File baseDir) {
        this(baseDir, 60, 16);
    }

    public FlexibleShardedCache(File baseDir, int defaultTtlMinutes, int shardCount) {
        this.baseDir = baseDir;
        this.shardCount = shardCount;
        this.defaultTtlMillis = TimeUnit.MINUTES.toMillis(defaultTtlMinutes);
        this.memoryMap = new ConcurrentHashMap<>();

        if (!baseDir.exists() && !baseDir.mkdirs()) {
            logger.warn("Failed to create cache directory: {}", baseDir);
        }

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        loadAllShards();
        // 定时同步
        this.scheduler.scheduleAtFixedRate(this::flushAllShards, 5, 5, TimeUnit.MINUTES);
        logger.info("FlexibleShardedCache initialized. BaseDir: {}, Shards: {}, DefaultTTL: {} mins", baseDir, shardCount, defaultTtlMinutes);
    }

    @Override
    public <T> T get(String key) {
        CacheEntry entry = memoryMap.get(key);
        if (entry == null)
            return null;
        if (entry.isExpired()) {
            memoryMap.remove(key);
            flushShardAsync(key);
            return null;
        }
        @SuppressWarnings("unchecked")
        T result = (T) entry.getValue();
        return result;
    }

    @Override
    public <T> void put(String key, T value) {
        put(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> void put(String key, T value, CacheStoreMode mode) {
        // 当前实现暂未拆分多后端，先保持旧行为兼容。
        put(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit) {
        if (value == null) {
            remove(key);
            return;
        }
        long expireTime = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        memoryMap.put(key, new CacheEntry(value, expireTime));
        flushShardAsync(key);
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit, CacheStoreMode mode) {
        // 当前实现暂未拆分多后端，先保持旧行为兼容。
        put(key, value, ttl, timeUnit);
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
}
