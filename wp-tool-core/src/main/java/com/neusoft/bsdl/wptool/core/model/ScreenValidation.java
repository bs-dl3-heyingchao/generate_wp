package com.neusoft.bsdl.wptool.core.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 画面チェック仕様書のコラム定義
 */
@Data
public class ScreenValidation {
	@ExcelProperty(value = "項番", index = 0)
	private String itemNo;

	@ExcelProperty(value = "項目名", index = 1)
	private String itemName;

	@ExcelProperty(value = "チェック名", index = 7)
	private String validationName;

	@ExcelProperty(value = "種別", index = 18)
	private String type;

	@ExcelProperty(value = "チェック仕様", index = 22)
	private String validationRule;
	
	@ExcelProperty(value = "チェックアクション", index = 50)
	private String validationAction;
	
	@ExcelProperty(value = "メッセージID", index = 74)
	private String massageId;
	
	@ExcelProperty(value = "メッセージ内容", index = 79)
	private String messageContent;
	
	@ExcelProperty(value = "{1}", index = 105)
	private String parameter1;
	
	@ExcelProperty(value = "{2}", index = 111)
	private String parameter2;
	
	@ExcelProperty(value = "{3}", index = 117)
	private String parameter3;
	
	@ExcelProperty(value = "{4}", index = 123)
	private String parameter4;

	@ExcelProperty(value = "{5}", index = 129)
	private String parameter5;
	
	@ExcelProperty(value = "備考", index = 135)
	private String remarks;
	
	@ExcelProperty(value = "IO,BP,ワーニング", index = 171)
	private String bizWarining;
	
	@ExcelProperty(value = "実装メモ", index = 172)
	private String codingMemo;
}