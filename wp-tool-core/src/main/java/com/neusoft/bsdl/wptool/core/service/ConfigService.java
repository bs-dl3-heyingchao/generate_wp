package com.neusoft.bsdl.wptool.core.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.neusoft.bsdl.wptool.core.exception.WPException;

/**
 * 共通設定取得サービス
 */
public class ConfigService {
    private static final Properties properties = new Properties();

    static {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = cl.getResourceAsStream("wp-tool-config.properties")) {
            if (inputStream != null) {
                properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            } else {
                throw new RuntimeException("未找到 wp-tool-config 文件，请确保它位于 src/main/resources 目录下");
            }
        } catch (IOException e) {
            throw new RuntimeException("无法加载 wp-tool-config", e);
        }
    }

    public static String getConfig(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = properties.getProperty(key);
        }
        if (value == null) {
            throw new WPException(String.format("未配置 %s，请在 wp-tool-config 文件中添加该配置项", key));
        }
        return value;
    }

    public static String getConfig(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = properties.getProperty(key);
        }
        return value == null ? defaultValue : value;
    }

    public static File getSvnBaseDir() {
        String baseDir = getConfig("wp-tool.svn.base-dir");
        File dir = new File(baseDir);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new WPException("SVN基準ディレクトリが存在しないか: " + baseDir);
        }
        return dir;
    }

    public static File getSvnFullPath(String relativePath) {
        File dir = getSvnBaseDir();
        return new File(dir, relativePath);
    }

    public static File getSvnDBDefineDir() {
        return getSvnFullPath(getConfig("wp-tool.svn.db-define.dir"));
    }

    public static File getDBDefineCacheFile() {
        return new File(getConfig("wp-tool.db.db-define.cachefile"));
    }

    public static File getSvnSessionItemDefineFile() {
        return getSvnFullPath(getConfig("wp-tool.svn.session-items-define.file"));
    }
    
    public static File getCacheDir() {
        File dir = new File(getConfig("wp-tool.cache-dir"));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}