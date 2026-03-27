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
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 简易分片持久化缓存 (无线程版本) 特点：所有读写操作同步进行，无后台线程，逻辑简单，适合单线程环境或小型工具。
 */
public class SimpleShardedCache {

    private static final Logger logger = LoggerFactory.getLogger(SimpleShardedCache.class);

    private final File baseDir;
    private final int shardCount;
    // 使用普通 HashMap，因为单线程访问不需要 ConcurrentHashMap
    private final Map<String, CacheEntry> memoryMap;
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

    public SimpleShardedCache(File baseDir) {
        this(baseDir, 60, 16);
    }

    public SimpleShardedCache(File baseDir, int defaultTtlMinutes, int shardCount) {
        this.baseDir = baseDir;
        this.shardCount = shardCount;
        this.defaultTtlMillis = TimeUnit.MINUTES.toMillis(defaultTtlMinutes);
        this.memoryMap = new HashMap<>();

        // 确保目录存在
        if (!baseDir.exists()) {
            if (baseDir.mkdirs()) {
                logger.info("Created cache directory: {}", baseDir);
            } else {
                logger.warn("Failed to create cache directory: {}", baseDir);
            }
        }

        // 启动时加载所有分片 (同步加载)
        loadAllShards();

        logger.info("SimpleShardedCache initialized. BaseDir: {}, Shards: {}, DefaultTTL: {} mins", baseDir, shardCount, defaultTtlMinutes);
    }

    /**
     * 放入数据 (同步写入磁盘)
     */
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

        // 同步触发分片写入 (无异步，直接执行)
        flushShard(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = memoryMap.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            memoryMap.remove(key);
            // 删除过期数据后，同步更新磁盘
            flushShard(key);
            return null;
        }
        return (T) entry.getValue();
    }

    /**
     * 移除数据 (同步写入磁盘)
     */
    public void remove(String key) {
        if (memoryMap.remove(key) != null) {
            flushShard(key);
        }
    }

    private int getShardIndex(String key) {
        return Math.abs(key.hashCode()) % shardCount;
    }

    private String getShardFilePath(int index) {
        return baseDir + File.separator + "cache_" + index + ".dat";
    }

    /**
     * 同步保存指定分片到磁盘
     */
    private void flushShard(String key) {
        int index = getShardIndex(key);
        flushShard(index);
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
                // 如果该分片没有有效数据，可以选择删除旧文件，这里选择跳过
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

            if (targetFile.exists()) {
                targetFile.delete();
            }

            if (!tempFile.renameTo(targetFile)) {
                logger.error("Failed to rename temp file to target: {} -> {}", tempPath, filePath);
            }

            long duration = System.currentTimeMillis() - start;
            logger.debug("[Shard {}] Saved {} items in {}ms", shardIndex, shardMap.size(), duration);

        } catch (IOException e) {
            logger.error("[Shard {}] Failed to save cache data: {}", shardIndex, e.getMessage(), e);
        }
    }

    /**
     * 加载所有分片 (启动时调用)
     */
    private void loadAllShards() {
        logger.info("Loading shards from {}...", baseDir);
        int totalLoaded = 0;

        for (int i = 0; i < shardCount; i++) {
            String filePath = getShardFilePath(i);
            File file = new File(filePath);
            if (!file.exists()) {
                continue;
            }

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

    /**
     * 关闭资源 (无线程池需要关闭，但可保留日志)
     */
    public void close() {
        logger.info("Cache closed.");
        // 由于没有后台线程，直接关闭即可。
        // 如果需要确保最后数据落盘，可以在这里调用一次 flushAllShards，
        // 但通常 put/remove 已经同步落盘了。
    }
}
