package com.neusoft.bsdl.wptool.core.io;

import java.io.IOException;
import java.io.InputStream;

public class SvnFileSource implements FileSource {
    private final String svnUrl;
    private final String username;
    private final String password;

    public SvnFileSource(String svnUrl, String username, String password) {
        this.svnUrl = svnUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }
}