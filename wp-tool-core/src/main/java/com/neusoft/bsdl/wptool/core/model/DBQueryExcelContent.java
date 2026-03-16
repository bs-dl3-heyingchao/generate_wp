package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import lombok.Data;

/**
 * DBQuery定義書のExcelの内容を保持するクラス
 */
@Data
public class DBQueryExcelContent {
	private List<DBQuerySheetContent> querySheetContents;
}
