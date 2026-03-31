package com.neusoft.bsdl.wptool.core.cache;

/**
 * 分片缓存种类。
 */
public enum ShardedCacheKind {
    DEFAULT("default"),
    DB("db"),
    PARSE_EXCEL("parse-excel");

    private final String dirName;

    ShardedCacheKind(String dirName) {
        this.dirName = dirName;
    }

    public String getDirName() {
        return dirName;
    }
}