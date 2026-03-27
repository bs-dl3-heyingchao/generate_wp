package com.neusoft.bsdl.wptool.core.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分片持久化缓存 (支持自定义 TTL + SLF4J 日志)
 */
public class FlexibleShardedCache implements AutoCloseable {

    // 1. 获取 Logger 实例
    private static final Logger logger = LoggerFactory.getLogger(FlexibleShardedCache.class);

    private final File baseDir;
    private final int shardCount;
    private final Map<String, CacheEntry> memoryMap;
    private final ScheduledExecutorService scheduler;
    private final long defaultTtlMillis;

    private static class CacheEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Object value;
        private final long expireTime;

        public CacheEntry(Object value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    public FlexibleShardedCache(File baseDir) {
        this(baseDir, 60, 16); // 默认16分片，默认TTL 60分钟
    }

    public FlexibleShardedCache(File baseDir, int defaultTtlMinutes, int shardCount) {
        this.baseDir = baseDir;
        this.shardCount = shardCount;
        this.defaultTtlMillis = TimeUnit.MINUTES.toMillis(defaultTtlMinutes);
        this.memoryMap = new ConcurrentHashMap<>();

        // 确保目录存在
        if (!baseDir.exists()) {
            if (baseDir.mkdirs()) {
                logger.info("Created cache directory: {}", baseDir);
            } else {
                logger.warn("Failed to create cache directory: {}", baseDir);
            }
        }

        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // 启动时加载所有分片
        loadAllShards();

        // 定时同步所有分片到磁盘
        this.scheduler.scheduleAtFixedRate(this::flushAllShards, 5, 5, TimeUnit.MINUTES);
        logger.info("ShardedCache initialized. BaseDir: {}, Shards: {}, DefaultTTL: {} mins", baseDir, shardCount, defaultTtlMinutes);
    }

    public <T> void put(String key, T value) {
        put(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS);
    }

    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit) {
        if (value == null) {
            remove(key);
            return;
        }

        long expireTime = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        memoryMap.put(key, new CacheEntry(value, expireTime));

        // 异步触发分片写入
        flushShardAsync(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = memoryMap.get(key);
        if (entry == null)
            return null;

        if (entry.isExpired()) {
            memoryMap.remove(key);
            flushShardAsync(key);
            // 可选：如果频繁读取过期数据，可以在 DEBUG 级别打印
            // logger.debug("Key {} expired and removed", key);
            return null;
        }
        return (T) entry.getValue();
    }

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
        CompletableFuture.runAsync(() -> flushShard(index));
    }

    private void flushShard(int shardIndex) {
        long start = System.currentTimeMillis();
        String filePath = getShardFilePath(shardIndex);

        try {
            Map<String, CacheEntry> shardMap = new HashMap<>();

            // 收集该分片的有效数据
            for (Map.Entry<String, CacheEntry> entry : memoryMap.entrySet()) {
                if (getShardIndex(entry.getKey()) == shardIndex && !entry.getValue().isExpired()) {
                    shardMap.put(entry.getKey(), entry.getValue());
                }
            }

            if (shardMap.isEmpty()) {
                // 如果该分片没有有效数据，可以选择删除旧文件，或者跳过
                // 这里选择跳过，保持文件结构稳定
                return;
            }

            // 写入临时文件
            String tempPath = filePath + ".tmp";
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempPath))) {
                oos.writeObject(shardMap);
            }

            // 原子替换
            File tempFile = new File(tempPath);
            File targetFile = new File(filePath);
            if (targetFile.exists())
                targetFile.delete();

            if (!tempFile.renameTo(targetFile)) {
                logger.error("Failed to rename temp file to target: {} -> {}", tempPath, filePath);
            }

            long duration = System.currentTimeMillis() - start;
            logger.debug("[Shard {}] Saved {} items in {}ms", shardIndex, shardMap.size(), duration);

        } catch (IOException e) {
            logger.error("[Shard {}] Failed to save cache data: {}", shardIndex, e.getMessage(), e);
        }
    }

    private void flushAllShards() {
        logger.debug("Flushing all shards...");
        for (int i = 0; i < shardCount; i++) {
            flushShard(i);
        }
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
                    int validCount = 0;
                    for (Map.Entry<String, CacheEntry> entry : loadedMap.entrySet()) {
                        if (!entry.getValue().isExpired()) {
                            memoryMap.put(entry.getKey(), entry.getValue());
                            validCount++;
                        }
                    }
                    totalLoaded += validCount;
                    if (validCount > 0) {
                        logger.info("Loaded Shard {}: {} valid items.", i, validCount);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Failed to load Shard {}: {}. Deleting corrupted file.", i, e.getMessage(), e);
                file.delete();
            }
        }
        logger.info("Total loaded: {} items.", totalLoaded);
    }

    @Override
    public void close() {
        logger.info("Shutting down cache...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Scheduler did not terminate gracefully, forcing shutdown.");
                scheduler.shutdownNow();
            }
            flushAllShards();
            logger.info("Cache closed successfully.");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            logger.error("Interrupted during shutdown.", e);
        }
    }
}
