package com.neusoft.bsdl.wptool.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

class WPGlobalUtilsTest {

    @Test
    void given固定文本流_when计算Md5_then返回预期哈希() throws IOException {
        var input = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));
        String md5 = WPGlobalUtils.calculateMd5(input);
        assertEquals("5d41402abc4b2a76b9719d911017c592", md5);
    }

    @Test
    void given固定文本文件_when计算Md5_then返回预期哈希() throws IOException {
        File tempFile = Files.createTempFile("md5-test", ".txt").toFile();
        Files.writeString(tempFile.toPath(), "hello", StandardCharsets.UTF_8);
        String md5 = WPGlobalUtils.calculateMd5(tempFile);
        assertEquals("5d41402abc4b2a76b9719d911017c592", md5);
    }

    @Test
    void given空入参_when计算Md5_then抛出参数异常() {
        assertThrows(IllegalArgumentException.class, () -> WPGlobalUtils.calculateMd5((ByteArrayInputStream) null));
        assertThrows(IllegalArgumentException.class, () -> WPGlobalUtils.calculateMd5((File) null));
    }
}
