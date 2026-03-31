package com.neusoft.bsdl.wptool.core.cache.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
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
    private final Map<String, CacheStoreMode> keyModeMap;
    private final Object cacheLock;
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
        this.keyModeMap = new ConcurrentHashMap<>();
        this.cacheLock = new Object();

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
        synchronized (cacheLock) {
            return getInternal(key, null, false);
        }
    }

    @Override
    public <T> T get(String key, String latestTag) {
        synchronized (cacheLock) {
            if (latestTag == null) {
                return getInternal(key, null, false);
            }
            return getInternal(key, latestTag, true);
        }
    }

    private <T> T getInternal(String key, String latestTag, boolean checkTag) {
        CacheEntry entry = memoryMap.get(key);
        if (entry != null) {
            if (entry.isExpired()) {
                memoryMap.remove(key);
                CacheStoreMode mode = keyModeMap.getOrDefault(key, CacheStoreMode.MEMORY_AND_DISK);
                if (writesDisk(mode)) {
                    removeFromDisk(key);
                }
                return null;
            }
            if (checkTag && !latestTag.equals(entry.getTag())) {
                return null;
            }
            @SuppressWarnings("unchecked")
            T result = (T) entry.getValue();
            return result;
        }

        CacheStoreMode mode = keyModeMap.get(key);
        if (mode == CacheStoreMode.MEMORY_ONLY) {
            return null;
        }

        CacheEntry diskEntry = loadFromDisk(key);
        if (diskEntry == null) {
            return null;
        }
        if (diskEntry.isExpired()) {
            memoryMap.remove(key);
            keyModeMap.remove(key);
            removeFromDisk(key);
            return null;
        }
        if (checkTag && !latestTag.equals(diskEntry.getTag())) {
            return null;
        }

        if (mode == null) {
            keyModeMap.put(key, CacheStoreMode.MEMORY_AND_DISK);
        } else if (writesMemory(mode)) {
            memoryMap.put(key, diskEntry);
        }

        @SuppressWarnings("unchecked")
        T result = (T) diskEntry.getValue();
        return result;
    }

    @Override
    public <T> void put(String key, T value) {
        synchronized (cacheLock) {
            putInternal(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS, CacheStoreMode.MEMORY_AND_DISK, null);
        }
    }

    @Override
    public <T> void put(String key, T value, String cacheTag) {
        synchronized (cacheLock) {
            putInternal(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS, CacheStoreMode.MEMORY_AND_DISK, cacheTag);
        }
    }

    @Override
    public <T> void put(String key, T value, CacheStoreMode mode) {
        synchronized (cacheLock) {
            putInternal(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS, mode, null);
        }
    }

    @Override
    public <T> void put(String key, T value, CacheStoreMode mode, String cacheTag) {
        synchronized (cacheLock) {
            putInternal(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS, mode, cacheTag);
        }
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit) {
        synchronized (cacheLock) {
            putInternal(key, value, ttl, timeUnit, CacheStoreMode.MEMORY_AND_DISK, null);
        }
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit, String cacheTag) {
        synchronized (cacheLock) {
            putInternal(key, value, ttl, timeUnit, CacheStoreMode.MEMORY_AND_DISK, cacheTag);
        }
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit, CacheStoreMode mode) {
        synchronized (cacheLock) {
            putInternal(key, value, ttl, timeUnit, mode, null);
        }
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit, CacheStoreMode mode, String cacheTag) {
        synchronized (cacheLock) {
            putInternal(key, value, ttl, timeUnit, mode, cacheTag);
        }
    }

    private <T> void putInternal(String key, T value, long ttl, TimeUnit timeUnit, CacheStoreMode mode, String cacheTag) {
        if (mode == null) {
            throw new IllegalArgumentException("CacheStoreMode must not be null");
        }
        if (value == null) {
            remove(key);
            return;
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit must not be null");
        }
        long expireTime = calculateExpireTime(ttl, timeUnit);
        CacheEntry cacheEntry = new CacheEntry(value, expireTime, cacheTag);
        keyModeMap.put(key, mode);

        if (writesMemory(mode)) {
            memoryMap.put(key, cacheEntry);
        } else {
            memoryMap.remove(key);
        }

        if (writesDisk(mode)) {
            saveToDisk(key, cacheEntry, mode);
        } else {
            removeFromDisk(key);
        }
    }

    @Override
    public void remove(String key) {
        synchronized (cacheLock) {
            CacheStoreMode mode = keyModeMap.remove(key);
            memoryMap.remove(key);
            if (mode == null || writesDisk(mode)) {
                removeFromDisk(key);
            }
        }
    }

    private boolean writesMemory(CacheStoreMode mode) {
        return mode == CacheStoreMode.MEMORY_ONLY || mode == CacheStoreMode.MEMORY_AND_DISK;
    }

    private boolean writesDisk(CacheStoreMode mode) {
        return mode == CacheStoreMode.DISK_ONLY || mode == CacheStoreMode.MEMORY_AND_DISK;
    }

    private int getShardIndex(String key) {
        return Math.abs(key.hashCode()) % shardCount;
    }

    private String getShardFilePath(int index) {
        return baseDir + File.separator + "cache_" + index + ".dat";
    }

    private void flushShard(int shardIndex) {
        long start = System.currentTimeMillis();
        String filePath = getShardFilePath(shardIndex);
        Map<String, CacheRecord> shardMap = loadShardMap(shardIndex);

        for (Map.Entry<String, CacheRecord> entry : new HashMap<>(shardMap).entrySet()) {
            String key = entry.getKey();
            CacheRecord record = entry.getValue();
            CacheStoreMode mode = keyModeMap.get(key);
            CacheEntry memoryEntry = memoryMap.get(key);

            if (mode == null && record != null) {
                mode = record.mode != null ? record.mode : CacheStoreMode.MEMORY_AND_DISK;
            }

            if (mode == null || !writesDisk(mode)) {
                shardMap.remove(key);
                continue;
            }

            if (memoryEntry == null || memoryEntry.isExpired()) {
                if (mode == CacheStoreMode.DISK_ONLY) {
                    if (record != null && record.entry != null && !record.entry.isExpired()) {
                        shardMap.put(key, record);
                    } else {
                        shardMap.remove(key);
                    }
                } else {
                    shardMap.remove(key);
                }
                continue;
            }

            shardMap.put(key, new CacheRecord(memoryEntry, mode));
        }

        for (Map.Entry<String, CacheEntry> entry : memoryMap.entrySet()) {
            String key = entry.getKey();
            if (getShardIndex(key) != shardIndex) {
                continue;
            }
            CacheEntry cacheEntry = entry.getValue();
            if (cacheEntry == null || cacheEntry.isExpired()) {
                continue;
            }
            CacheStoreMode mode = keyModeMap.getOrDefault(key, CacheStoreMode.MEMORY_AND_DISK);
            if (writesDisk(mode)) {
                shardMap.put(key, new CacheRecord(cacheEntry, mode));
            }
        }

        persistShardMap(filePath, shardMap);
        logger.debug("[Shard {}] Saved {} items in {}ms", shardIndex, shardMap.size(), System.currentTimeMillis() - start);
    }

    private Map<String, CacheRecord> loadShardMap(int shardIndex) {
        String filePath = getShardFilePath(shardIndex);
        File file = new File(filePath);
        Map<String, CacheRecord> shardMap = new HashMap<>();
        if (!file.exists()) {
            return shardMap;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object loaded = ois.readObject();
            if (loaded instanceof Map<?, ?> loadedMap) {
                for (Map.Entry<?, ?> entry : loadedMap.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        continue;
                    }
                    Object value = entry.getValue();
                    if (value instanceof CacheRecord record) {
                        if (record.entry != null && !record.entry.isExpired()) {
                            shardMap.put(key, record);
                        }
                    } else if (value instanceof CacheEntry cacheEntry) {
                        if (!cacheEntry.isExpired()) {
                            shardMap.put(key, new CacheRecord(cacheEntry, CacheStoreMode.MEMORY_AND_DISK));
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Failed to load Shard {}: {}. Deleting.", shardIndex, e.getMessage());
            file.delete();
        }
        return shardMap;
    }

    private CacheEntry loadFromDisk(String key) {
        int shardIndex = getShardIndex(key);
        Map<String, CacheRecord> shardMap = loadShardMap(shardIndex);
        CacheRecord record = shardMap.get(key);
        if (record == null || record.entry == null) {
            return null;
        }
        if (record.entry.isExpired()) {
            shardMap.remove(key);
            persistShardMap(getShardFilePath(shardIndex), shardMap);
            return null;
        }
        return record.entry;
    }

    private void saveToDisk(String key, CacheEntry cacheEntry, CacheStoreMode mode) {
        int shardIndex = getShardIndex(key);
        Map<String, CacheRecord> shardMap = loadShardMap(shardIndex);
        shardMap.put(key, new CacheRecord(cacheEntry, mode));
        persistShardMap(getShardFilePath(shardIndex), shardMap);
    }

    private void removeFromDisk(String key) {
        int shardIndex = getShardIndex(key);
        Map<String, CacheRecord> shardMap = loadShardMap(shardIndex);
        if (shardMap.remove(key) != null) {
            persistShardMap(getShardFilePath(shardIndex), shardMap);
        }
    }

    private void persistShardMap(String filePath, Map<String, CacheRecord> shardMap) {
        File targetFile = new File(filePath);
        if (shardMap.isEmpty()) {
            if (targetFile.exists() && !targetFile.delete()) {
                logger.warn("Failed to delete empty shard file: {}", filePath);
            }
            return;
        }

        String tempPath = filePath + ".tmp";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempPath))) {
            oos.writeObject(shardMap);
        } catch (IOException e) {
            logger.error("Failed to persist shard file: {}", filePath, e);
            return;
        }

        File tempFile = new File(tempPath);
        if (targetFile.exists() && !targetFile.delete()) {
            logger.warn("Failed to delete old shard file before rename: {}", filePath);
        }
        if (!tempFile.renameTo(targetFile)) {
            logger.error("Failed to rename temp file: {} -> {}", tempPath, filePath);
        }
    }

    private void flushAllShards() {
        synchronized (cacheLock) {
            for (int i = 0; i < shardCount; i++) {
                flushShard(i);
            }
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
                Object loaded = ois.readObject();
                if (loaded instanceof Map<?, ?> loadedMap) {
                    for (Map.Entry<?, ?> entry : loadedMap.entrySet()) {
                        if (!(entry.getKey() instanceof String key)) {
                            continue;
                        }
                        Object value = entry.getValue();
                        if (value instanceof CacheRecord record) {
                            if (record.entry == null || record.entry.isExpired()) {
                                continue;
                            }
                            keyModeMap.put(key, record.mode == null ? CacheStoreMode.MEMORY_AND_DISK : record.mode);
                            if (writesMemory(keyModeMap.get(key))) {
                                memoryMap.put(key, record.entry);
                            }
                            totalLoaded++;
                        } else if (value instanceof CacheEntry cacheEntry) {
                            if (!cacheEntry.isExpired()) {
                                memoryMap.put(key, cacheEntry);
                                keyModeMap.put(key, CacheStoreMode.MEMORY_AND_DISK);
                                totalLoaded++;
                            }
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

    private static final class CacheRecord implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final CacheEntry entry;
        private final CacheStoreMode mode;

        private CacheRecord(CacheEntry entry, CacheStoreMode mode) {
            this.entry = entry;
            this.mode = mode;
        }
    }
}
