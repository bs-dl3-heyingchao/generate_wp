package com.neusoft.bsdl.wptool.core.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 画面設計書定義
 */
@Data
public class ScreenExcelContent {
    /** 画面項目説明書 */
    private Map<String, List<ScreenItemDescriptionResult>> screenItemDesMaps;
    /** 画面機能定義書 */
    private List<ScreenFuncSpecification> screenFuncSpecification;
    /** 画面チェック仕様書 */
    private ScreenValidation screenValidation;
    /** CSVレイアウト */
    private CsvLayout csvLayout;
    /** DB設定項目定義 */
    private Map<String, List<DBConfigItemDefinition>> dbConfigItemDefinitionMaps;
}
