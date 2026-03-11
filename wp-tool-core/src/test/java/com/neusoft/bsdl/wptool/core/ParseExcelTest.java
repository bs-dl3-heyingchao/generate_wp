package com.neusoft.bsdl.wptool.core;

import java.io.File;
import java.util.List;

import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.reader.WPTableBeanReader;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;

import cbai.util.FileUtil;
import cbai.util.db.define.TableBean;
import cbai.util.db.define.reader.ITableBeanReader;
import tools.jackson.databind.ObjectMapper;

public class ParseExcelTest {

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Please pass input file name as the first argument.");
        }

        FileSource source = new LocalFileSource(args[0]);
        ScreenExcelContent screenExcelContent = ParseExcelUtils.parseScreenExcel(source);
        ObjectMapper objectMapper = new ObjectMapper();
        File outputFile = new File("./target/output.json");
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, screenExcelContent);
        System.out.println("JSON output: " + outputFile.getPath());
    }
    
}
