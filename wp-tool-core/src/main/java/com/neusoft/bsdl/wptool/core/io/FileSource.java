package com.neusoft.bsdl.wptool.core.io;

import java.io.IOException;
import java.io.InputStream;

public interface FileSource {
    InputStream getInputStream() throws IOException;
}