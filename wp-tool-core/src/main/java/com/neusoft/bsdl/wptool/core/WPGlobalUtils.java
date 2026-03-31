package com.neusoft.bsdl.wptool.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.neusoft.bsdl.wptool.core.cache.ShardedCache;
import com.neusoft.bsdl.wptool.core.cache.ShardedCacheKind;
import com.neusoft.bsdl.wptool.core.cache.impl.SimpleShardedCache;
import com.neusoft.bsdl.wptool.core.service.ConfigService;

public class WPGlobalUtils {
    private static final Map<String, ShardedCache> shardedCacheMap = new ConcurrentHashMap<>();

    private WPGlobalUtils() {
    }

    public static void setShardedCache(ShardedCacheKind kind, ShardedCache cache) {
        String cacheKind = normalizeKind(kind);
        if (cache == null) {
            ShardedCache existing = shardedCacheMap.remove(cacheKind);
            closeQuietly(existing);
            return;
        }

        ShardedCache previous = shardedCacheMap.put(cacheKind, cache);
        if (previous != null && previous != cache) {
            closeQuietly(previous);
        }
    }

    public static ShardedCache getShardedCache(ShardedCacheKind kind) {
        String cacheKind = normalizeKind(kind);
        return shardedCacheMap.computeIfAbsent(cacheKind, WPGlobalUtils::createShardedCache);

    }

    public static String calculateMd5(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }

        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available", e);
        }

        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            messageDigest.update(buffer, 0, read);
        }

        byte[] digest = messageDigest.digest();
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    public static String calculateMd5(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            return calculateMd5(inputStream);
        }
    }

    private static ShardedCache createShardedCache(String kind) {
        File cacheDir = new File(ConfigService.getCacheDir(), kind);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return new SimpleShardedCache(cacheDir);
    }

    private static String normalizeKind(ShardedCacheKind kind) {
        return kind.getDirName();
    }

    private static void closeQuietly(ShardedCache cache) {
        if (cache == null) {
            return;
        }
        try {
            cache.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
