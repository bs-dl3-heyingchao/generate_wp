package com.neusoft.bsdl.wptool.core.model;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
public class SessionManagementSystemField {
	/** 分類 */
	@ExcelProperty(index = 0)
	private String category;

	/** セッションキー */
	@ExcelProperty(index = 1)
	private String sessionKey;

	/** 論理名 */
	@ExcelProperty(index = 2)
	private String sessionLogicName;
	
	/** セクション名称 */
	private String sectionName;
}
