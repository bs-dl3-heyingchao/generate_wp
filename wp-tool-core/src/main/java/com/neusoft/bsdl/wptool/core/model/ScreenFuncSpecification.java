package com.neusoft.bsdl.wptool.core.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 画面機能定義書のコラム定義
 */
@Data
public class ScreenFuncSpecification {
	@ExcelProperty(value = "項番", index = 0)
    private String itemNo;
	
    @ExcelProperty(value = "機能名", index =1)
    private String functionName;

    @ExcelProperty(value = "機能説明", index = 13)
    private String functionDescription;

    @ExcelProperty(value = "項目名", index = 40)
    private String itemName;
    
    @ExcelProperty(value = "備考", index = 52)
    private String remarks;
    
    @ExcelProperty(value = "実装メモ", index = 76)
    private String codingMemo;
}