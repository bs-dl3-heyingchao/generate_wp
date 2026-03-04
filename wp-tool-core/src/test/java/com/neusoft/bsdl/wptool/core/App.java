package com.neusoft.bsdl.wptool.core;

import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;

import tools.jackson.databind.ObjectMapper;

public class App {

    public static void main(String[] args) throws Exception {
        FileSource source = new LocalFileSource("D:\\128SYIS25142\\test\\（内部設計書サンプル）MTI340S01B01_画面設計書_汎用テーブル一覧.xlsx");
        ScreenExcelContent screenExcelContent = ParseExcelUtils.parseScreenExcel(source);
        ObjectMapper objectMapper = new ObjectMapper();
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(screenExcelContent);
        System.out.println(prettyJson);
    }
}
