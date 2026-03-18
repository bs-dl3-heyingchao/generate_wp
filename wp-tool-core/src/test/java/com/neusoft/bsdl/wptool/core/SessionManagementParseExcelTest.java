package com.neusoft.bsdl.wptool.core;

import java.io.File;

import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.SessionManagementContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;

import tools.jackson.databind.ObjectMapper;

public class SessionManagementParseExcelTest {

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Please pass input file name as the first argument.");
        }
        
        FileSource source = new LocalFileSource(args[0]);
        SessionManagementContent sessionManagementContent = ParseExcelUtils.parseSessionManagementExcel(source);
        ObjectMapper objectMapper = new ObjectMapper();
        File outputFile = new File("./target/output.json");
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, sessionManagementContent);
        System.out.println("JSON output: " + outputFile.getPath());
    }
    
}
