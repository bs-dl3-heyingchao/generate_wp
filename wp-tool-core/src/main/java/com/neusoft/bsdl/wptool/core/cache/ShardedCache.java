package com.neusoft.bsdl.wptool.core.cache;

import java.util.concurrent.TimeUnit;

/**
 * 分片持久化缓存统一接口 支持放入、获取、移除数据，并支持自定义 TTL。
 */
public interface ShardedCache extends AutoCloseable {

    /**
     * 获取默认 TTL 的缓存值
     * 
     * @param key 键
     * @return 值，如果不存在或过期则返回 null
     */
    <T> T get(String key);

    /**
     * 获取缓存值，并根据最新标识字符串进行一致性校验。
     *
     * @param key       键
     * @param latestTag 最新标识字符串（如 MD5）；为 null 时按旧行为直接返回缓存
     * @return 值，如果不存在、过期或标识不一致则返回 null
     */
    default <T> T get(String key, String latestTag) {
        return get(key);
    }

    /**
     * 放入缓存，使用默认 TTL
     * 
     * @param key   键
     * @param value 值 (null 会触发删除操作)
     */
    <T> void put(String key, T value);

    /**
     * 放入缓存，使用默认 TTL，并附带自定义标识字符串。
     *
     * @param key      键
     * @param value    值 (null 会触发删除操作)
     * @param cacheTag 自定义标识字符串（如 MD5）
     */
    default <T> void put(String key, T value, String cacheTag) {
        put(key, value);
    }

    /**
     * 放入缓存，使用默认 TTL，并指定存储模式
     *
     * @param key   键
     * @param value 值 (null 会触发删除操作)
     * @param mode  存储模式
     */
    <T> void put(String key, T value, CacheStoreMode mode);

    /**
     * 放入缓存，使用默认 TTL，并指定存储模式和自定义标识字符串。
     *
     * @param key      键
     * @param value    值 (null 会触发删除操作)
     * @param mode     存储模式
     * @param cacheTag 自定义标识字符串（如 MD5）
     */
    default <T> void put(String key, T value, CacheStoreMode mode, String cacheTag) {
        put(key, value, mode);
    }

    /**
     * 放入缓存，使用自定义 TTL
     * 
     * @param key      键
     * @param value    值
     * @param ttl      有效期时长
     * @param timeUnit 时间单位
     */
    <T> void put(String key, T value, long ttl, TimeUnit timeUnit);

    /**
     * 放入缓存，使用自定义 TTL，并指定存储模式
     *
     * @param key      键
     * @param value    值
     * @param ttl      有效期时长
     * @param timeUnit 时间单位
     * @param mode     存储模式
     */
    <T> void put(String key, T value, long ttl, TimeUnit timeUnit, CacheStoreMode mode);

    /**
     * 移除缓存
     * 
     * @param key 键
     */
    void remove(String key);

}