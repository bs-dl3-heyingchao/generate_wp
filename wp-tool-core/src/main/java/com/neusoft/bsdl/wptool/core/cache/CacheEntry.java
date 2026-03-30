package com.neusoft.bsdl.wptool.core.cache;

import java.io.Serializable;

public class CacheEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Object value;
    private final long expireTime;

    public CacheEntry(Object value, long expireTime) {
        this.value = value;
        this.expireTime = expireTime;
    }

    public Object getValue() {
        return value;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }
}