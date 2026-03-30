package com.neusoft.bsdl.wptool.core;

import com.neusoft.bsdl.wptool.core.cache.ShardedCache;
import com.neusoft.bsdl.wptool.core.cache.impl.SimpleShardedCache;
import com.neusoft.bsdl.wptool.core.service.ConfigService;

public class WPGlobalUtils {
    private static ShardedCache shardedCache = null;

    private WPGlobalUtils() {
    }

    public static synchronized void setShardedCache(ShardedCache cache) {
        if (shardedCache != null) {
            try {
                shardedCache.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        shardedCache = cache;

    }

    public static synchronized ShardedCache getShardedCache() {
        if (shardedCache != null) {
            return shardedCache;
        }
        shardedCache = new SimpleShardedCache(ConfigService.getCacheDir());
        return shardedCache;

    }
}
