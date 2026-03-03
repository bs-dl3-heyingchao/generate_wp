package com.neusoft.bsdl.wptool.core;

import java.util.List;

import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescription;
import com.neusoft.bsdl.wptool.core.service.GamenItemExplainParseExcel;

public class App {

	public static void main(String[] args) throws Exception {
        FileSource source = new LocalFileSource("D:\\128SYIS25142\\test\\（内部設計書サンプル）MTI340S01B01_画面設計書_汎用テーブル一覧.xlsx");
        GamenItemExplainParseExcel service = new GamenItemExplainParseExcel();

        List<ScreenItemDescription> fields = service.parseSpecSheet(source, "画面項目説明書");
         System.out.println("excel.size:"+fields.size()); 
        for (ScreenItemDescription field : fields) {
            System.out.println(field.getItemNo() + " | " + field.getFieldName());
        }
    }
}
