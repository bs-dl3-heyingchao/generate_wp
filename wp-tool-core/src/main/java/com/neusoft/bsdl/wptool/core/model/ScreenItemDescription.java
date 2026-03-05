package com.neusoft.bsdl.wptool.core.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 画面項目説明書のコラム定義
 */
@Data
public class ScreenItemDescription{
	@ExcelProperty(value = "項目番号", index = 0)
    private String itemNo;
	
    @ExcelProperty(value = "項目名", index = 1)
    private String itemName;

    @ExcelProperty(value = "I/O", index = 10)
    private String io;

    @ExcelProperty(value = "必須", index = 12)
    private String required;
    
    @ExcelProperty(value = "表示", index = 14)
    private String display;
    
    @ExcelProperty(value = "属性(WP)", index = 16)
    private String attributeWP;
    
    @ExcelProperty(value = "属性", index = 19)
    private String attribute;

    @ExcelProperty(value = "桁数(WP)", index =22)
    private String lengthWP;
    
    @ExcelProperty(value = "桁数", index = 25)
    private String length;

    @ExcelProperty(value = "ソート順", index = 28)
    private String sorted;
    
    @ExcelProperty(value = "データモデル名", index = 30)
    private String modelName;
    
    @ExcelProperty(value = "項目名", index = 38)
    private String modelItemName;

    @ExcelProperty(value = "フォーマット", index = 46)
    private String format;

    @ExcelProperty(value = "初期値", index = 56)
    private String defaultValue;

    @ExcelProperty(value = "編集仕様", index = 70)
    private String editRule;

    @ExcelProperty(value = "加工式", index = 80)
    private String processingRule;

    @ExcelProperty(value = "選択リスト", index = 97)
    private String selectList;

    @ExcelProperty(value = "表示条件", index = 107)
    private String displayCondition;

    @ExcelProperty(value = "備考", index = 117)
    private String remarks;

    @ExcelProperty(value = "WP項目名", index = 128)
    private String wpItemName;
}