package com.neusoft.bsdl.wptool.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ローカルファイルを読み込む
 */
public class LocalFileSource implements FileSource {
	private final String filePath;

    public LocalFileSource(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(new File(filePath));
    }
}
