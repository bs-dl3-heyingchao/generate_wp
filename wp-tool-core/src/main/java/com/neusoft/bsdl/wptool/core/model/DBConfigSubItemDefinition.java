package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

/**
 * DB設定項目定義の一覧の解析内容
 */
@Data
public class DBConfigSubItemDefinition implements Serializable {
	private static final long serialVersionUID = 1L;
	@ExcelProperty(value = "項目", index = 0)
	private String logicalName;
	@ExcelProperty(value = "", index = 11)
	private String physicalName;
	@ExcelProperty(value = "設定内容", index = 26)
	private String configContent;
	@ExcelProperty(value = "備考", index = 46)
	private String remarks;
}
