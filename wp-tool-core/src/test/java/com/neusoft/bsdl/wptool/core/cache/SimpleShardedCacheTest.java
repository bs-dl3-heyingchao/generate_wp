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

import com.neusoft.bsdl.wptool.core.cache.impl.SimpleShardedCache;
import com.neusoft.bsdl.wptool.core.cache.store.CacheShardStore;

class SimpleShardedCacheTest {

    /**
     * Given: 传入空的 CacheShardStore
     * When:  构造 SimpleShardedCache
     * Then:  抛出 IllegalArgumentException
     */
    @Test
    void given空Store_when构造缓存_then抛出参数异常() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleShardedCache((CacheShardStore) null, 60, 1));
    }

    /**
     * Given: 基础文件缓存实例和默认配置
     * When:  执行普通写入、带Tag写入与读取
     * Then:  命中规则正确，且默认TTL为无限大
     */
    @Test
    void given默认配置_when基础读写与Tag校验_then命中正确且默认TTL无限() throws Exception {
        var tempDir = Files.createTempDirectory("simple-sharded-cache-test").toFile();
        try (var cache = new SimpleShardedCache(tempDir);
                var cacheWithArgs = new SimpleShardedCache(tempDir, 1, 1)) {
            cache.put("k1", "v1");
            assertEquals("v1", cache.get("k1"));

            cache.put("tagKey", "tagValue", "md5-abc");
            assertEquals("tagValue", cache.get("tagKey", "md5-abc"));
            assertNull(cache.get("tagKey", "md5-def"));
            assertEquals("tagValue", cache.get("tagKey", null));

            Map<String, CacheEntry> memoryMap = getMemoryMap(cache);
            assertEquals(Long.MAX_VALUE, getExpireTime(memoryMap.get("k1")));
            assertTrue(cacheWithArgs != null);
        }
    }

    /**
     * Given: 可用缓存实例
     * When:  按不同模式写入并覆盖TTL/空值边界
     * Then:  模式行为、过期行为与参数校验符合预期
     */
    @Test
    void given缓存实例_when按模式与TTL写入_then行为与参数校验正确() throws Exception {
        var tempDir = Files.createTempDirectory("simple-sharded-cache-test").toFile();
        try (var cache = new SimpleShardedCache(tempDir)) {
            cache.put("k2", "v2", CacheStoreMode.MEMORY_ONLY);
            assertEquals("v2", cache.get("k2"));

            Map<String, CacheEntry> memoryMap = getMemoryMap(cache);
            memoryMap.remove("k2");
            assertNull(cache.get("k2"));

            cache.put("k3", "v3", CacheStoreMode.DISK_ONLY);
            assertEquals("v3", cache.get("k3"));

            cache.put("k4", "v4", -1, TimeUnit.MILLISECONDS);
            assertNull(cache.get("k4"));

            cache.put("k4_tag", "v4_tag", -1, TimeUnit.MILLISECONDS, "md5-expired");
            assertNull(cache.get("k4_tag", "md5-expired"));

            cache.put("k5", null, CacheStoreMode.MEMORY_AND_DISK);
            assertNull(cache.get("k5"));

            assertThrows(IllegalArgumentException.class,
                    () -> cache.put("k6", "v6", 1, TimeUnit.SECONDS, (CacheStoreMode) null));
        }
    }

    /**
     * Given: 预置分片数据（含新鲜、过期、遗留数据）
     * When:  加载缓存并执行Tag一致性校验
     * Then:  新鲜命中、过期失效、遗留模式回填正确
     */
    @Test
    void given预置分片数据_when加载并按Tag读取_then命中过期与遗留兼容正确() throws Exception {
        var store = new ControlledStore();
        store.shards.put(0, new HashMap<>());
        store.shards.get(0).put("loadedFresh", new CacheEntry("fv", System.currentTimeMillis() + 30_000));
        store.shards.get(0).put("loadedExpired", new CacheEntry("ev", System.currentTimeMillis() - 1));
        store.loadFailedShards.add(1);

        try (var cache = new SimpleShardedCache(store, 1, 2)) {
            assertTrue(store.initialized);
            assertEquals("fv", cache.get("loadedFresh"));
            assertNull(cache.get("loadedExpired"));
            assertNull(cache.get("absent"));

            cache.put("tagDiskKey", "tagDiskValue", CacheStoreMode.MEMORY_AND_DISK, "md5-x");
            assertEquals("tagDiskValue", cache.get("tagDiskKey", "md5-x"));
            assertNull(cache.get("tagDiskKey", "md5-y"));
            assertEquals("tagDiskValue", cache.get("tagDiskKey", null));

            store.shards.computeIfAbsent(0, k -> new HashMap<>())
                    .put("legacyDisk", new CacheEntry("legacyV", System.currentTimeMillis() + 30_000));
            assertEquals("legacyV", cache.get("legacyDisk"));

            Map<String, CacheStoreMode> keyModeMap = getKeyModeMap(cache);
            assertEquals(CacheStoreMode.MEMORY_AND_DISK, keyModeMap.get("legacyDisk"));

            keyModeMap.remove("legacyDisk");
            cache.remove("legacyDisk");
        }
    }

    /**
     * Given: 受控Store构造器与返回null分片的Store
     * When:  分别使用两种Store进行构造
     * Then:  构造流程可完成且基础读取行为符合预期
     */
    @Test
    void given不同Store实现_when构造缓存_then构造与基础读取符合预期() {
        var ctorStore = new ControlledStore();
        ctorStore.shards.put(0, new HashMap<>());
        ctorStore.shards.get(0).put("loadedFresh", new CacheEntry("fv", System.currentTimeMillis() + 30_000));
        ctorStore.shards.get(0).put("loadedExpired", new CacheEntry("ev", System.currentTimeMillis() - 1));
        try (var storeCtorCache = new SimpleShardedCache(ctorStore)) {
            assertEquals("fv", storeCtorCache.get("loadedFresh"));
            assertNull(storeCtorCache.get("loadedExpired"));
        }

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
        try (var nullMapCtorCache = new SimpleShardedCache(nullMapStore, 1, 1)) {
            assertTrue(nullMapCtorCache != null);
        }
    }

    /**
     * Given: 可注入失败场景的受控Store
     * When:  执行flush路径与读写异常分支
     * Then:  异常分支可被触发且抛出预期异常
     */
    @Test
    void given受控失败场景_when执行Flush与异常路径_then异常分支覆盖完整() throws Exception {
        var store = new ControlledStore();
        store.loadFailedShards.add(1);
        try (var cache = new SimpleShardedCache(store, 1, 2)) {
            Map<String, CacheEntry> memoryMap = getMemoryMap(cache);
            Map<String, CacheStoreMode> keyModeMap = getKeyModeMap(cache);

            cache.put("memOnlyRemove", "x", CacheStoreMode.MEMORY_ONLY);
            cache.remove("memOnlyRemove");

            memoryMap.put("expiredNoDisk", new CacheEntry("x", System.currentTimeMillis() - 1));
            keyModeMap.put("expiredNoDisk", CacheStoreMode.MEMORY_AND_DISK);
            store.shards.computeIfAbsent(0, k -> new HashMap<>()).remove("expiredNoDisk");
            assertNull(cache.get("expiredNoDisk"));

            memoryMap.put("expiredMemoryOnly", new CacheEntry("x", System.currentTimeMillis() - 1));
            keyModeMap.put("expiredMemoryOnly", CacheStoreMode.MEMORY_ONLY);
            assertNull(cache.get("expiredMemoryOnly"));

            Method flushShardMethod = getFlushShardMethod();
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
            assertThrows(IllegalStateException.class,
                    () -> cache.put("persistErrorKey", "pv", CacheStoreMode.MEMORY_AND_DISK));
            store.failPersist = false;

            keyModeMap.put("removeErrorKey", CacheStoreMode.MEMORY_AND_DISK);
            store.failLoad = true;
            assertThrows(IllegalStateException.class, () -> cache.remove("removeErrorKey"));
            store.failLoad = false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, CacheEntry> getMemoryMap(SimpleShardedCache cache) throws Exception {
        Field memoryMapField = SimpleShardedCache.class.getDeclaredField("memoryMap");
        memoryMapField.setAccessible(true);
        return (Map<String, CacheEntry>) memoryMapField.get(cache);
    }

    @SuppressWarnings("unchecked")
    private Map<String, CacheStoreMode> getKeyModeMap(SimpleShardedCache cache) throws Exception {
        Field keyModeMapField = SimpleShardedCache.class.getDeclaredField("keyModeMap");
        keyModeMapField.setAccessible(true);
        return (Map<String, CacheStoreMode>) keyModeMapField.get(cache);
    }

    private long getExpireTime(CacheEntry cacheEntry) throws Exception {
        Field expireTimeField = CacheEntry.class.getDeclaredField("expireTime");
        expireTimeField.setAccessible(true);
        return expireTimeField.getLong(cacheEntry);
    }

    private Method getFlushShardMethod() throws Exception {
        Method flushShardMethod = SimpleShardedCache.class.getDeclaredMethod("flushShard", String.class);
        flushShardMethod.setAccessible(true);
        return flushShardMethod;
    }

    private static class ControlledStore implements CacheShardStore {
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
}
