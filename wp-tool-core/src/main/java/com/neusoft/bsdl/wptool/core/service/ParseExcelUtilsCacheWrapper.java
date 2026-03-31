package com.neusoft.bsdl.wptool.core.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.neusoft.bsdl.wptool.core.WPGlobalUtils;
import com.neusoft.bsdl.wptool.core.cache.CacheStoreMode;
import com.neusoft.bsdl.wptool.core.cache.ShardedCache;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.DBQueryExcelContent;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;

public final class ParseExcelUtilsCacheWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(ParseExcelUtilsCacheWrapper.class);
    private static final long CACHE_TTL_DAYS = 1L;
    private static final String SCREEN_CACHE_KEY_PREFIX = "parse-excel:screen:";
    private static final String DBQUERY_CACHE_KEY_PREFIX = "parse-excel:dbquery:";

    private ParseExcelUtilsCacheWrapper() {
    }

    public static ScreenExcelContent parseScreenExcel(FileSource source) throws Exception {
        return cacheAndParse(source, SCREEN_CACHE_KEY_PREFIX, ParseExcelUtils::parseScreenExcel);
    }

    public static DBQueryExcelContent parseDBQueryExcel(FileSource source) throws Exception {
        return cacheAndParse(source, DBQUERY_CACHE_KEY_PREFIX, ParseExcelUtils::parseDBQueryExcel);
    }

    private static <T> T cacheAndParse(FileSource source, String cacheKeyPrefix, ExcelParser<T> parser) throws Exception {
        byte[] inputBytes = readAllBytes(source);
        String md5 = WPGlobalUtils.calculateMd5(new ByteArrayInputStream(inputBytes));
        String cacheKey = cacheKeyPrefix + md5;

        ShardedCache cache = WPGlobalUtils.getShardedCache();
        T cachedValue = cache.get(cacheKey, md5);
        if (cachedValue != null) {
            LOG.info("ParseExcel cache hit, key={}", cacheKey);
            return cachedValue;
        }

        LOG.info("ParseExcel cache miss, key={}", cacheKey);
        T parsedValue = parser.parse(new ByteArrayFileSource(inputBytes));
        cache.put(cacheKey, parsedValue, CACHE_TTL_DAYS, TimeUnit.DAYS, CacheStoreMode.DISK_ONLY, md5);
        return parsedValue;
    }

    private static byte[] readAllBytes(FileSource source) throws IOException {
        try (InputStream inputStream = source.getInputStream(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            inputStream.transferTo(outputStream);
            return outputStream.toByteArray();
        }
    }

    private interface ExcelParser<T> {
        T parse(FileSource source) throws Exception;
    }

    private static final class ByteArrayFileSource implements FileSource {
        private final byte[] bytes;

        private ByteArrayFileSource(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }
    }
}