package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;

import lombok.Data;

/**
 * 解析仕様書のシートごとのコンテンツ
 */
@Data
public class ExcelSheetContent<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	//シート名称
	private String sheetName;
	//解析結果シートコンテンツ
    private T content;
}
