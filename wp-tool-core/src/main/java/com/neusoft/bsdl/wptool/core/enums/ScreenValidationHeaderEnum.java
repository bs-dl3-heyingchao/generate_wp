// com.neusoft.bsdl.wptool.core.enums.ScreenValidationHeaderEnum.java
package com.neusoft.bsdl.wptool.core.enums;

/**
 * 画面チェック仕様書のヘッダー列定義
 */
public enum ScreenValidationHeaderEnum implements HeaderEnum {
	ITEM_NO(0,"項番", 0), ITEM_NAME(0,"項目名", 1), VALIDATION_NAME(0,"チェック名", 7), TYPE(0,"種別", 18), VALIDATION_RULE(0,"チェック仕様", 22),
	MESSAGE_ID(0,"メッセージID", 80), MESSAGE_CONTENT(0,"メッセージ内容", 85), PARAMETER_1(1,"{1}", 111), PARAMETER_2(1,"{2}", 117),
	PARAMETER_3(1,"{3}", 123), PARAMETER_4(1,"{4}", 129), PARAMETER_5(1,"{5}", 135), REMARKS(0,"備考", 141),
	BIZ_WARNING(0,"IO,BP,ワーニング", 177), CODING_MEMO(0,"実装メモ", 178);
	
	private final int level;
	private final String displayName;
	private final int columnIndex;

	ScreenValidationHeaderEnum(int level,String displayName, int columnIndex) {
		this.level = level;
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
	
	public int getLevel() {
		return level;
	}
}