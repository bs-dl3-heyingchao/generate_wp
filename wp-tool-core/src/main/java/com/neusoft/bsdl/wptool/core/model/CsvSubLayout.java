package com.neusoft.bsdl.wptool.core.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * CSVレイアウトのコラム定義
 */
@Data
public class CsvSubLayout {
	@ExcelProperty(value = "項番", index = 0)
	private String itemNo;

	@ExcelProperty(value = "項目名", index = 1)
	private String filedName;

	@ExcelProperty(value = "表示", index = 11)
	private String validationName;

	@ExcelProperty(value = "属性(WP)", index = 13)
	private String attributeWP;

	@ExcelProperty(value = "属性", index = 16)
	private String attribute;
	
	@ExcelProperty(value = "桁数(WP)", index = 19)
	private String lengthWP;
	
	@ExcelProperty(value = "桁数", index = 22)
	private String length;
	
	@ExcelProperty(value = "固定値", index = 25)
	private String f5Modify;

	@ExcelProperty(value = "表示順", index = 34)
	private String sorted;

	@ExcelProperty(value = "テーブル名", index = 36)
	private String tableName;

	@ExcelProperty(value = "項目名", index = 46)
	private String itemName;

	@ExcelProperty(value = "備考", index = 56)
	private String remarks;
}