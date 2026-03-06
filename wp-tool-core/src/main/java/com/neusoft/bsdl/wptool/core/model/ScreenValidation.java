package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import com.alibaba.excel.annotation.ExcelProperty;
import com.google.common.collect.Lists;

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

	// チェックアクションエリア
	private List<ScreenValidationAction> validationActions = Lists.newArrayList();

	@ExcelProperty(value = "メッセージID", index = 80)
	private String massageId;

	@ExcelProperty(value = "メッセージ内容", index = 85)
	private String messageContent;

	@ExcelProperty(value = "{1}", index = 111)
	private String parameter1;

	@ExcelProperty(value = "{2}", index = 117)
	private String parameter2;

	@ExcelProperty(value = "{3}", index = 123)
	private String parameter3;

	@ExcelProperty(value = "{4}", index = 129)
	private String parameter4;

	@ExcelProperty(value = "{5}", index = 135)
	private String parameter5;

	@ExcelProperty(value = "備考", index = 141)
	private String remarks;

	@ExcelProperty(value = "IO,BP,ワーニング", index = 177)
	private String bizWarining;

	@ExcelProperty(value = "実装メモ", index = 178)
	private String codingMemo;
}