package com.neusoft.bsdl.wptool.core.cache.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.neusoft.bsdl.wptool.core.cache.CacheEntry;

/**
 * 基于本地文件系统的分片缓存持久化实现。
 */
public class LocalFileCacheShardStore implements CacheShardStore {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileCacheShardStore.class);

    private final File baseDir;

    public LocalFileCacheShardStore(File baseDir) {
        if (baseDir == null) {
            throw new IllegalArgumentException("baseDir must not be null");
        }
        this.baseDir = baseDir;
    }

    @Override
    public void initialize() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            logger.warn("Failed to create cache directory: {}", baseDir);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, CacheEntry> loadShard(int shardIndex) throws IOException, ClassNotFoundException {
        File file = new File(getShardFilePath(shardIndex));
        if (!file.exists()) {
            return new HashMap<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof Map<?, ?>) {
                return (Map<String, CacheEntry>) obj;
            }
            return new HashMap<>();
        }
    }

    @Override
    public void persistShard(int shardIndex, Map<String, CacheEntry> shardMap) throws IOException {
        String filePath = getShardFilePath(shardIndex);
        File targetFile = new File(filePath);

        if (shardMap.isEmpty()) {
            if (targetFile.exists() && !targetFile.delete()) {
                throw new IOException("Failed to delete empty shard file: " + filePath);
            }
            return;
        }

        String tempPath = filePath + ".tmp";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempPath))) {
            oos.writeObject(shardMap);
        }

        File tempFile = new File(tempPath);
        if (targetFile.exists() && !targetFile.delete()) {
            throw new IOException("Failed to replace shard file: " + filePath);
        }
        if (!tempFile.renameTo(targetFile)) {
            throw new IOException("Failed to rename temp file: " + tempPath + " -> " + filePath);
        }
    }

    @Override
    public String getStoreDescription() {
        return baseDir.getAbsolutePath();
    }

    private String getShardFilePath(int index) {
        return baseDir + File.separator + "cache_" + index + ".dat";
    }
}
