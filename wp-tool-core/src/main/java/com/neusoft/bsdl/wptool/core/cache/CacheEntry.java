package com.neusoft.bsdl.wptool.core.cache;

import java.io.Serializable;

public class CacheEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Object value;
    private final long expireTime;
    private final String tag;

    public CacheEntry(Object value, long expireTime) {
        this(value, expireTime, null);
    }

    public CacheEntry(Object value, long expireTime, String tag) {
        this.value = value;
        this.expireTime = expireTime;
        this.tag = tag;
    }

    public Object getValue() {
        return value;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }

    public String getTag() {
        return tag;
    }
}