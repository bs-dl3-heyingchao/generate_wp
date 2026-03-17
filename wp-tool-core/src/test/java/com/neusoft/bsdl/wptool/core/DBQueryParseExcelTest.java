package com.neusoft.bsdl.wptool.core;

import java.io.File;

import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.DBQueryExcelContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;

import tools.jackson.databind.ObjectMapper;

public class DBQueryParseExcelTest {

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Please pass input file name as the first argument.");
        }

        FileSource source = new LocalFileSource(args[0]);
        DBQueryExcelContent queryExcelContent = ParseExcelUtils.parseDBQueryExcel(source);
        System.out.println("JSON output: " + queryExcelContent);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("./target/queryExcelContent.json"), queryExcelContent);
    }
    
}
