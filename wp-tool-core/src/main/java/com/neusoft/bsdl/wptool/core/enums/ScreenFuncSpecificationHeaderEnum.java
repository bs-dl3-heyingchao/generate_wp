package com.neusoft.bsdl.wptool.core.enums;

/**
* 画面機能仕様書のヘッダー列定義
*/
public enum ScreenFuncSpecificationHeaderEnum implements HeaderEnum {
 ITEM_NO("項番", 0),
 FUNCTION_NAME("機能名", 1),
 FUNCTION_DESCRIPTION("機能説明", 13),
 ITEM_NAME("項目名", 40),
 REMARKS("備考", 52),
 CODING_MEMO("実装メモ", 76);

 private final String displayName;
 private final int columnIndex;

 ScreenFuncSpecificationHeaderEnum(String displayName, int columnIndex) {
     this.displayName = displayName;
     this.columnIndex = columnIndex;
 }

 @Override
 public String getDisplayName() {
     return displayName;
 }

 @Override
 public int getColumnIndex() {
     return columnIndex;
 }
}