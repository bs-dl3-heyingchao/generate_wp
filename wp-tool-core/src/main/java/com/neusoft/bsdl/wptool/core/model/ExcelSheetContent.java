package com.neusoft.bsdl.wptool.core.model;

import lombok.Data;

/**
 * 解析仕様書のシートごとのコンテンツ
 */
@Data
public class ExcelSheetContent<T>{
	//シート名称
	private String sheetName;
	//解析結果シートコンテンツ
    private T content;
}
