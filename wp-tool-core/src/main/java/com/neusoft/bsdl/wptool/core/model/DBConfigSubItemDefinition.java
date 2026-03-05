package com.neusoft.bsdl.wptool.core.model;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

/**
 * DB設定項目定義のコラム定義
 */
@Data
public class DBConfigSubItemDefinition {
	@ExcelProperty(value = "項目", index = 0)
	private String item;
	@ExcelProperty(value = "設定内容", index = 13)
	private String configContent;
	@ExcelProperty(value = "備考", index = 44)
	private String remarks;
}
