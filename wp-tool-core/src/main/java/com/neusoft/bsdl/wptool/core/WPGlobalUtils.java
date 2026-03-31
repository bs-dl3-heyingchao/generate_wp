package com.neusoft.bsdl.wptool.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
}
