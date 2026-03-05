package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import lombok.Data;

/**
 * 解析仕様書のコンテンツ
 */
@Data
public class ScreenExcelContent{
	//システム
	private String system;
	//サブシステム
	private String subSystem;
	//フェーズ
	private String phase;
	//ドキュメント名
	private String documentName;
	//機能分類
	private String functionType;
	//機能ID
	private String functionId;
	//画面ID
	private String screenId;
	//画面名
	private String screenName;
	//解析結果シートコンテンツ
    private List<ExcelSheetContent<?>> sheetList;
}
