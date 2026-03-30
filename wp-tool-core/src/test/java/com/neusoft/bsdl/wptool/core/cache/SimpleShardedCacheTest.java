package com.neusoft.bsdl.wptool.core.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.neusoft.bsdl.wptool.core.cache.entry.CacheEntry;
import com.neusoft.bsdl.wptool.core.cache.impl.SimpleShardedCache;
import com.neusoft.bsdl.wptool.core.cache.store.CacheShardStore;

class SimpleShardedCacheTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldCoverSimpleShardedCacheAllBranches() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new SimpleShardedCache((CacheShardStore) null, 60, 1));

        var tempDir = Files.createTempDirectory("simple-sharded-cache-test").toFile();
        var fileCtorCache = new SimpleShardedCache(tempDir);
        var fileCtorWithArgsCache = new SimpleShardedCache(tempDir, 1, 1);

        fileCtorCache.put("k1", "v1");
        assertEquals("v1", fileCtorCache.get("k1"));

        fileCtorCache.put("k2", "v2", CacheStoreMode.MEMORY_ONLY);
        assertEquals("v2", fileCtorCache.get("k2"));

        fileCtorCache.put("k3", "v3", CacheStoreMode.DISK_ONLY);
        assertEquals("v3", fileCtorCache.get("k3"));

        fileCtorCache.put("k4", "v4", -1, TimeUnit.MILLISECONDS);
        assertNull(fileCtorCache.get("k4"));

        fileCtorCache.put("k5", null, CacheStoreMode.MEMORY_AND_DISK);
        assertNull(fileCtorCache.get("k5"));

        assertThrows(IllegalArgumentException.class,
                () -> fileCtorCache.put("k6", "v6", 1, TimeUnit.SECONDS, null));

        fileCtorCache.remove("k2");
        fileCtorCache.close();
        fileCtorWithArgsCache.close();

        class ControlledStore implements CacheShardStore {
            private final Map<Integer, Map<String, CacheEntry>> shards = new HashMap<>();
            private final Set<Integer> loadFailedShards = new HashSet<>();
            private boolean failLoad;
            private boolean failPersist;
            private boolean initialized;

            @Override
            public void initialize() {
                initialized = true;
            }

            @Override
            public Map<String, CacheEntry> loadShard(int shardIndex) throws IOException {
                if (failLoad || loadFailedShards.contains(shardIndex)) {
                    loadFailedShards.remove(shardIndex);
                    throw new IOException("load failed");
                }
                return new HashMap<>(shards.getOrDefault(shardIndex, new HashMap<>()));
            }

            @Override
            public void persistShard(int shardIndex, Map<String, CacheEntry> shardMap) throws IOException {
                if (failPersist) {
                    throw new IOException("persist failed");
                }
                shards.put(shardIndex, new HashMap<>(shardMap));
            }

            @Override
            public String getStoreDescription() {
                return "controlled-store";
            }
        }

        var ctorStore = new ControlledStore();
        ctorStore.shards.put(0, new HashMap<>());
        ctorStore.shards.get(0).put("loadedFresh", new CacheEntry("fv", System.currentTimeMillis() + 30_000));
        ctorStore.shards.get(0).put("loadedExpired", new CacheEntry("ev", System.currentTimeMillis() - 1));
        var storeCtorCache = new SimpleShardedCache(ctorStore);
        assertEquals("fv", storeCtorCache.get("loadedFresh"));
        storeCtorCache.close();

        CacheShardStore nullMapStore = new CacheShardStore() {
            @Override
            public void initialize() {
            }

            @Override
            public Map<String, CacheEntry> loadShard(int shardIndex) {
                return null;
            }

            @Override
            public void persistShard(int shardIndex, Map<String, CacheEntry> shardMap) {
            }

            @Override
            public String getStoreDescription() {
                return "null-map-store";
            }
        };
        var nullMapCtorCache = new SimpleShardedCache(nullMapStore, 1, 1);
        nullMapCtorCache.close();

        var store = new ControlledStore();
        store.loadFailedShards.add(1);
        var cache = new SimpleShardedCache(store, 1, 2);
        assertTrue(store.initialized);

        assertNull(cache.get("absent"));

        cache.put("memoryOnlyKey", "mv", CacheStoreMode.MEMORY_ONLY);
        Field memoryMapField = SimpleShardedCache.class.getDeclaredField("memoryMap");
        memoryMapField.setAccessible(true);
        Map<String, CacheEntry> memoryMap = (Map<String, CacheEntry>) memoryMapField.get(cache);
        memoryMap.remove("memoryOnlyKey");
        assertNull(cache.get("memoryOnlyKey"));

        store.shards.computeIfAbsent(0, k -> new HashMap<>()).put("expiredDisk", new CacheEntry("dv", System.currentTimeMillis() - 1));
        assertNull(cache.get("expiredDisk"));

        store.shards.computeIfAbsent(0, k -> new HashMap<>()).put("legacyDisk", new CacheEntry("legacyV", System.currentTimeMillis() + 30_000));
        assertEquals("legacyV", cache.get("legacyDisk"));

        Field keyModeMapField = SimpleShardedCache.class.getDeclaredField("keyModeMap");
        keyModeMapField.setAccessible(true);
        Map<String, CacheStoreMode> keyModeMap = (Map<String, CacheStoreMode>) keyModeMapField.get(cache);
        assertEquals(CacheStoreMode.MEMORY_AND_DISK, keyModeMap.get("legacyDisk"));

        keyModeMap.remove("legacyDisk");
        cache.remove("legacyDisk");

        cache.put("memOnlyRemove", "x", CacheStoreMode.MEMORY_ONLY);
        cache.remove("memOnlyRemove");

        memoryMap.put("expiredNoDisk", new CacheEntry("x", System.currentTimeMillis() - 1));
        keyModeMap.put("expiredNoDisk", CacheStoreMode.MEMORY_AND_DISK);
        store.shards.computeIfAbsent(0, k -> new HashMap<>()).remove("expiredNoDisk");
        assertNull(cache.get("expiredNoDisk"));

        memoryMap.put("expiredMemoryOnly", new CacheEntry("x", System.currentTimeMillis() - 1));
        keyModeMap.put("expiredMemoryOnly", CacheStoreMode.MEMORY_ONLY);
        assertNull(cache.get("expiredMemoryOnly"));

        Method flushShardMethod = SimpleShardedCache.class.getDeclaredMethod("flushShard", String.class);
        flushShardMethod.setAccessible(true);

        memoryMap.put("flushWriteBack", new CacheEntry("live", System.currentTimeMillis() + 30_000));
        keyModeMap.put("flushWriteBack", CacheStoreMode.MEMORY_AND_DISK);
        flushShardMethod.invoke(cache, "flushWriteBack");

        memoryMap.put("flushExpiredEntry", new CacheEntry("old", System.currentTimeMillis() - 1));
        keyModeMap.put("flushExpiredEntry", CacheStoreMode.MEMORY_AND_DISK);
        flushShardMethod.invoke(cache, "flushExpiredEntry");

        memoryMap.put("flushNoDiskMode", new CacheEntry("n", System.currentTimeMillis() + 30_000));
        keyModeMap.put("flushNoDiskMode", CacheStoreMode.MEMORY_ONLY);
        flushShardMethod.invoke(cache, "flushNoDiskMode");

        store.failLoad = true;
        assertThrows(IllegalStateException.class, () -> cache.get("loadErrorKey"));
        store.failLoad = false;

        memoryMap.put("expiredForFlushError", new CacheEntry("x", System.currentTimeMillis() - 1));
        keyModeMap.put("expiredForFlushError", CacheStoreMode.MEMORY_AND_DISK);
        store.failLoad = true;
        assertThrows(IllegalStateException.class, () -> cache.get("expiredForFlushError"));
        store.failLoad = false;

        store.failPersist = true;
        assertThrows(IllegalStateException.class, () -> cache.put("persistErrorKey", "pv", CacheStoreMode.MEMORY_AND_DISK));
        store.failPersist = false;

        keyModeMap.put("removeErrorKey", CacheStoreMode.MEMORY_AND_DISK);
        store.failLoad = true;
        assertThrows(IllegalStateException.class, () -> cache.remove("removeErrorKey"));
        store.failLoad = false;

        cache.close();
    }
}
