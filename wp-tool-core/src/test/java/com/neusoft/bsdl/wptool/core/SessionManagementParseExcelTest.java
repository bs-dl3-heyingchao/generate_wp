package com.neusoft.bsdl.wptool.core;

import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.SessionManagementContent;
import com.neusoft.bsdl.wptool.core.service.SessionManagementParseExcel;

public class SessionManagementParseExcelTest {

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Please pass input file name as the first argument.");
        }
        FileSource source = new LocalFileSource(args[0]);
        SessionManagementParseExcel sessionManagementParseExcel= new SessionManagementParseExcel();
        SessionManagementContent sessionManagementContent = sessionManagementParseExcel.parseSpecSheet(source, "セッション項目一覧");
        System.out.println(sessionManagementContent.toString());
    }
    
}
