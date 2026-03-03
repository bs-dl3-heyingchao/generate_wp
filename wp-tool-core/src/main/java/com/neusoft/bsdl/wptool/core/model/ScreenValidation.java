package com.neusoft.bsdl.wptool.core.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 画面機能定義書のコラム定義
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
	
	@ExcelProperty(value = "F5 修正", index = 50)
	private String f5Modify;
	
	@ExcelProperty(value = "F4 照会", index = 52)
	private String f4Search;
	
	@ExcelProperty(value = "", index = 54)
	private String validationFuctionSpace1;
	
	@ExcelProperty(value = "", index = 56)
	private String validationFuctionSpace2;
	
	@ExcelProperty(value = "", index = 58)
	private String validationFuctionSpace3;
	
	@ExcelProperty(value = "", index = 60)
	private String validationFuctionSpace4;
	
	@ExcelProperty(value = "", index = 62)
	private String validationFuctionSpace5;
	
	@ExcelProperty(value = "メッセージID", index = 62)
	private String messageId;
}