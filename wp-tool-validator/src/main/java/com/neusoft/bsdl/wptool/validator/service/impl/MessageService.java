package com.neusoft.bsdl.wptool.validator.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 支持加载多个 message.properties 并合并
 */
public class MessageService {
    private static final Properties properties = new Properties();

    static {
        try {
            // 获取所有名为 "message.properties" 的资源（包括 A、B 工程的）
            Enumeration<URL> resources = MessageService.class.getClassLoader()
                    .getResources("message.properties");

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (InputStream inputStream = url.openStream()) {
                    properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                }
            }

            if (properties.isEmpty()) {
                throw new RuntimeException("未找到任何 message.properties 文件");
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