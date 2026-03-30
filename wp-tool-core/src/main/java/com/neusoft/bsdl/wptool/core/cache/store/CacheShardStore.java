package com.neusoft.bsdl.wptool.core.cache.store;

import java.io.IOException;
import java.util.Map;

import com.neusoft.bsdl.wptool.core.cache.CacheEntry;

/**
 * 分片缓存持久化后端接口，可替换为本地文件、数据库、对象存储等实现。
 */
public interface CacheShardStore {

    /**
     * 初始化后端资源。
     */
    void initialize();

    /**
     * 读取指定分片数据。
     *
     * @param shardIndex 分片索引
     * @return 分片数据，不存在时返回空 Map
     */
    Map<String, CacheEntry> loadShard(int shardIndex) throws IOException, ClassNotFoundException;

    /**
     * 持久化指定分片数据。
     *
     * @param shardIndex 分片索引
     * @param shardMap 分片数据
     */
    void persistShard(int shardIndex, Map<String, CacheEntry> shardMap) throws IOException;

    /**
     * 用于日志展示的后端描述。
     */
    String getStoreDescription();
}
