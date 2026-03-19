package com.neusoft.bsdl.wptool.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.Data;

/**
 * ローカルファイルを読み込む
 */
@Data
public class LocalFileSource implements FileSource {
    private final File file;

    public LocalFileSource(String filePath) {
        this(new File(filePath));
    }

    public LocalFileSource(File file) {
        this.file = file;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    public String getFileName() {
        return file.getName();
    }
}
