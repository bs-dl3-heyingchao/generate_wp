package com.neusoft.bsdl.wptool.core.model;

import lombok.Data;

/**
 * DB設定項目定義のコラム定義
 */
@Data
public class DBConfigSubItemDefinition {
	// 項目
	private String item;
	// 設定内容
	private String configValue;
	// 備考
	private String remarks;
}
