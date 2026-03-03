package com.neusoft.bsdl.wptool.core.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 仕様書の解析コンテンツ
 */
@Data
public class ParseExcelContent {
	//画面項目説明書
	private Map<String,List<ScreenItemDescription>> screenItemDesList;
	//画面機能定義書
	private ScreenFuncSpecification screenFuncSpecification;
	//画面チェック仕様書
	private ScreenValidation screenValidation;
	//CSVレイアウト
	private CsvLayout csvLayout;
	//DB設定項目定義
	private  Map<String,List<DBConfigItemDefinition>> dbConfigItemDefinitionList;
}
