package com.neusoft.bsdl.wptool.core.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * CSVレイアウトのコラム定義
 */
@Data
public class CsvLayout {
	@ExcelProperty(value = "項番", index = 0)
	private String itemNo;

	@ExcelProperty(value = "項目名", index = 1)
	private String filedName;

	private CsvSubLayout csvSubLayout;
}