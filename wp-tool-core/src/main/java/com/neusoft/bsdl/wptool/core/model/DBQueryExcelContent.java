package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * DBQuery定義書のExcelの内容を保持するクラス
 */
@Data
public class DBQueryExcelContent implements Serializable {
	private static final long serialVersionUID = 1L;
	private List<DBQuerySheetContent> querySheetContents;
}
