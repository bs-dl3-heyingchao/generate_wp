package com.neusoft.bsdl.wptool.core.model;


import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
public class ScreenMetadata {
    @ExcelProperty("システム")
    private String system;

    @ExcelProperty("サブシステム")
    private String subSystem;

    @ExcelProperty("フェーズ")
    private String phase;

    @ExcelProperty("ドキュメント名")
    private String documentName;

    @ExcelProperty("機能分類")
    private String functionType;

    @ExcelProperty("機能ID")
    private String functionId;

    @ExcelProperty("画面ID")
    private String screenId;

    @ExcelProperty("画面名")
    private String screenName;
}