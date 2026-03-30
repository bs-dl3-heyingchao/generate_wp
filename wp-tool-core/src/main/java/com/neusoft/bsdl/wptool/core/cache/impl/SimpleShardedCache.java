package com.neusoft.bsdl.wptool.core.cache.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.neusoft.bsdl.wptool.core.cache.CacheStoreMode;
import com.neusoft.bsdl.wptool.core.cache.ShardedCache;
import com.neusoft.bsdl.wptool.core.cache.entry.CacheEntry;
import com.neusoft.bsdl.wptool.core.cache.store.CacheShardStore;
import com.neusoft.bsdl.wptool.core.cache.store.LocalFileCacheShardStore;

/**
 * 简易分片持久化缓存 (同步写入，无后台线程) 适合单线程环境或对并发要求不高的场景。
 */
public class SimpleShardedCache implements ShardedCache {

    private static final Logger logger = LoggerFactory.getLogger(SimpleShardedCache.class);

    private final CacheShardStore shardStore;
    private final int shardCount;
    private final Map<String, CacheEntry> memoryMap;
    // 记录每个 key 的存储模式，支持仅内存/仅本地/双写。
    private final Map<String, CacheStoreMode> keyModeMap;
    private final long defaultTtlMillis;

    public SimpleShardedCache(File baseDir) {
        this(new LocalFileCacheShardStore(baseDir), 60, 16);
    }

    public SimpleShardedCache(File baseDir, int defaultTtlMinutes, int shardCount) {
        this(new LocalFileCacheShardStore(baseDir), defaultTtlMinutes, shardCount);
    }

    public SimpleShardedCache(CacheShardStore shardStore) {
        this(shardStore, 60, 16);
    }

    public SimpleShardedCache(CacheShardStore shardStore, int defaultTtlMinutes, int shardCount) {
        if (shardStore == null) {
            throw new IllegalArgumentException("CacheShardStore must not be null");
        }
        this.shardStore = shardStore;
        this.shardCount = shardCount;
        this.defaultTtlMillis = TimeUnit.MINUTES.toMillis(defaultTtlMinutes);
        this.memoryMap = new HashMap<>();
        this.keyModeMap = new HashMap<>();

        this.shardStore.initialize();

        loadAllShards();
        logger.info("SimpleShardedCache initialized. Store: {}, Shards: {}, DefaultTTL: {} mins", shardStore.getStoreDescription(), shardCount, defaultTtlMinutes);
    }

    @Override
    public <T> T get(String key) {
        CacheEntry entry = memoryMap.get(key);
        if (entry != null) {
            if (entry.isExpired()) {
                memoryMap.remove(key);
                CacheStoreMode mode = keyModeMap.getOrDefault(key, CacheStoreMode.MEMORY_AND_DISK);
                if (writesDisk(mode)) {
                    flushShard(key);
                }
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
            removeFromDisk(key);
            return null;
        }

        // 兼容旧数据：本地存在但没有模式时按双写对待。
        if (mode == null) {
            keyModeMap.put(key, CacheStoreMode.MEMORY_AND_DISK);
        }

        @SuppressWarnings("unchecked")
        T result = (T) diskEntry.getValue();
        return result;
    }

    @Override
    public <T> void put(String key, T value) {
        put(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS, CacheStoreMode.MEMORY_AND_DISK);
    }

    @Override
    public <T> void put(String key, T value, CacheStoreMode mode) {
        put(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS, mode);
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit) {
        put(key, value, ttl, timeUnit, CacheStoreMode.MEMORY_AND_DISK);
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit, CacheStoreMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("CacheStoreMode must not be null");
        }
        if (value == null) {
            remove(key);
            return;
        }

        long expireTime = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        CacheEntry cacheEntry = new CacheEntry(value, expireTime);
        keyModeMap.put(key, mode);

        if (writesMemory(mode)) {
            memoryMap.put(key, cacheEntry);
        } else {
            memoryMap.remove(key);
        }

        if (writesDisk(mode)) {
            saveToDisk(key, cacheEntry);
        } else {
            removeFromDisk(key);
        }
    }

    @Override
    public void remove(String key) {
        CacheStoreMode mode = keyModeMap.remove(key);
        memoryMap.remove(key);

        // 模式未知时同时尝试本地删除，兼容历史数据。
        if (mode == null || writesDisk(mode)) {
            removeFromDisk(key);
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

    private void flushShard(String key) {
        int shardIndex = getShardIndex(key);

        try {
            Map<String, CacheEntry> shardMap = loadShardMap(shardIndex);
            boolean changed;

            CacheStoreMode mode = keyModeMap.getOrDefault(key, CacheStoreMode.MEMORY_AND_DISK);
            CacheEntry memoryEntry = memoryMap.get(key);

            if (!writesDisk(mode) || memoryEntry == null || memoryEntry.isExpired()) {
                changed = shardMap.remove(key) != null;
            } else {
                shardMap.put(key, memoryEntry);
                changed = true;
            }

            if (changed) {
                persistShardMap(shardIndex, shardMap);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to flush shard for key: " + key, e);
        }
    }

    private void saveToDisk(String key, CacheEntry entry) {
        int shardIndex = getShardIndex(key);

        try {
            Map<String, CacheEntry> shardMap = loadShardMap(shardIndex);
            shardMap.put(key, entry);
            persistShardMap(shardIndex, shardMap);
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to save cache entry to disk for key: " + key, e);
        }
    }

    private void removeFromDisk(String key) {
        int shardIndex = getShardIndex(key);

        try {
            Map<String, CacheEntry> shardMap = loadShardMap(shardIndex);
            if (shardMap.remove(key) != null) {
                persistShardMap(shardIndex, shardMap);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to remove cache entry from disk for key: " + key, e);
        }
    }

    private CacheEntry loadFromDisk(String key) {
        try {
            Map<String, CacheEntry> shardMap = loadShardMap(getShardIndex(key));
            return shardMap.get(key);
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to read cache entry from disk for key: " + key, e);
        }
    }

    private Map<String, CacheEntry> loadShardMap(int shardIndex) throws IOException, ClassNotFoundException {
        return shardStore.loadShard(shardIndex);
    }

    private void persistShardMap(int shardIndex, Map<String, CacheEntry> shardMap) throws IOException {
        long start = System.currentTimeMillis();
        shardStore.persistShard(shardIndex, shardMap);

        logger.debug("[Shard {}] Saved {} items in {}ms", shardIndex, shardMap.size(), System.currentTimeMillis() - start);
    }

    private void loadAllShards() {
        logger.info("Loading shards from {}...", shardStore.getStoreDescription());
        int totalLoaded = 0;
        for (int i = 0; i < shardCount; i++) {
            try {
                Map<String, CacheEntry> loadedMap = loadShardMap(i);
                if (loadedMap != null) {
                    for (Map.Entry<String, CacheEntry> entry : loadedMap.entrySet()) {
                        if (!entry.getValue().isExpired()) {
                            memoryMap.put(entry.getKey(), entry.getValue());
                            keyModeMap.put(entry.getKey(), CacheStoreMode.MEMORY_AND_DISK);
                            totalLoaded++;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Failed to load Shard {}: {}", i, e.getMessage());
            }
        }
        logger.info("Total loaded: {} items.", totalLoaded);
    }

    @Override
    public void close() {
        logger.info("SimpleShardedCache closed.");
    }
}
