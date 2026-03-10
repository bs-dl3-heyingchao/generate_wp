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

    public static void main1(String[] args) throws Exception {
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
    
    public static void main(String[] args) {
        File cahceFile = new File("./target/wp_db.cache");
        String basePath = "D:\\WORK\\128SYIS25142_devora\\SVN_CITS\\documents\\04_内部設計\\02_作成ドキュメント\\50_機能設計\\80_DB設計書\\02_テーブル定義\\";
//        String excelDir = "./db/第４．０版";
        ITableBeanReader reader = new WPTableBeanReader(basePath);
        List<TableBean> list = reader.readTableList();
        list.forEach(t -> {
            System.out.println(t.getTableFullName());
        });
        FileUtil.writeObjectToFile(list, cahceFile);  
//        FileUtil.writeString(new File("./target/output.json"), JSONUtil.toStringPretty(list));  
    }
}
