package com.neusoft.bsdl.wptool.core.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 简易分片持久化缓存 (同步写入，无后台线程) 适合单线程环境或对并发要求不高的场景。
 */
public class SimpleShardedCache implements ShardedCache {

    private static final Logger logger = LoggerFactory.getLogger(SimpleShardedCache.class);

    private final File baseDir;
    private final int shardCount;
    // 使用普通 HashMap，单线程安全
    private final Map<String, CacheEntry> memoryMap;
    private final long defaultTtlMillis;

    public SimpleShardedCache(File baseDir) {
        this(baseDir, 60, 16);
    }

    public SimpleShardedCache(File baseDir, int defaultTtlMinutes, int shardCount) {
        this.baseDir = baseDir;
        this.shardCount = shardCount;
        this.defaultTtlMillis = TimeUnit.MINUTES.toMillis(defaultTtlMinutes);
        this.memoryMap = new HashMap<>();

        if (!baseDir.exists() && !baseDir.mkdirs()) {
            logger.warn("Failed to create cache directory: {}", baseDir);
        }

        loadAllShards();
        logger.info("SimpleShardedCache initialized. BaseDir: {}, Shards: {}, DefaultTTL: {} mins", baseDir, shardCount, defaultTtlMinutes);
    }

    @Override
    public <T> T get(String key) {
        CacheEntry entry = memoryMap.get(key);
        if (entry == null)
            return null;
        if (entry.isExpired()) {
            memoryMap.remove(key);
            flushShard(key); // 同步写入
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
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit) {
        if (value == null) {
            remove(key);
            return;
        }
        long expireTime = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        memoryMap.put(key, new CacheEntry(value, expireTime));
        flushShard(key); // 同步写入
    }

    @Override
    public void remove(String key) {
        if (memoryMap.remove(key) != null) {
            flushShard(key); // 同步写入
        }
    }

    private int getShardIndex(String key) {
        return Math.abs(key.hashCode()) % shardCount;
    }

    private String getShardFilePath(int index) {
        return baseDir + File.separator + "cache_" + index + ".dat";
    }

    private void flushShard(String key) {
        flushShard(getShardIndex(key));
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
        logger.info("SimpleShardedCache closed.");
        // 无需关闭线程池，因为本身就是同步操作
    }
}
