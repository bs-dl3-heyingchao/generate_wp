package com.neusoft.bsdl.wptool.core.service;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 共通メッセージ取得サービス
 */
public class MessageService {
    private static final Properties properties = new Properties();

    static {
        try (InputStream inputStream = MessageService.class.getClassLoader()
                .getResourceAsStream("message.properties")) {
            if (inputStream != null) {
                properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            } else {
                throw new RuntimeException("未找到 message.properties 文件，请确保它位于 src/main/resources 目录下");
            }
        } catch (IOException e) {
            throw new RuntimeException("无法加载 message.properties", e);
        }
    }

    public static String getMessage(String key) {
        return properties.getProperty(key, "MISSING_MESSAGE_KEY: " + key);
    }

    public static String getMessage(String key, Object... args) {
        String msg = properties.getProperty(key);
        if (msg == null) {
            return "MISSING_MESSAGE_KEY: " + key;
        }
        return String.format(msg, args);
    }
}