package com.neusoft.bsdl.wptool.core.cache;

/**
 * 缓存存储策略。
 */
public enum CacheStoreMode {
    /** 内存和本地文件都写入。 */
    MEMORY_AND_DISK,
    /** 仅写入内存，不落盘。 */
    MEMORY_ONLY,
    /** 仅写入本地文件，不驻留内存。 */
    DISK_ONLY
}
