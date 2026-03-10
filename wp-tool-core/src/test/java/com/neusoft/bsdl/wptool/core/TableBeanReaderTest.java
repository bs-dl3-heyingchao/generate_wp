package com.neusoft.bsdl.wptool.core;

import java.io.File;
import java.util.List;

import com.neusoft.bsdl.wptool.core.reader.WPTableBeanReader;

import cbai.util.FileUtil;
import cbai.util.db.define.TableBean;
import cbai.util.db.define.reader.ITableBeanReader;

public class TableBeanReaderTest {

    private static final String SVN_BASE_PATH = "D:\\WORK\\128SYIS25142_devora";

    public static void main(String[] args) {
        File cahceFile = new File("./target/wp_db.cache");
        String basePath = SVN_BASE_PATH + "\\SVN_CITS\\documents\\04_内部設計\\02_作成ドキュメント\\50_機能設計\\80_DB設計書\\02_テーブル定義\\";
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
