package com.neusoft.bsdl.wptool.core.enums;

/**
 * 画面項目説明書のヘッダ情報列挙
 */
public enum ScreenItemDescriptionHeaderEnum  implements HeaderEnum{
	ITEM_NO(0,"項番", 0), ITEM_NAME(1,"項目名", 1), IO(1,"I/O", 10), REQUIRED(1,"必須", 12), DISPLAY(1,"表示", 14),
	ATTRIBUTE_WP(1,"属性(WP)", 16), ATTRIBUTE(1,"属性", 19), LENGTH_WP(1,"桁数(WP)", 22), LENGTH(1,"桁数", 25), SORTED(1,"ソート順", 28),
	MODEL_NAME(1,"データモデル名", 30), MODEL_ITEM_NAME(1,"項目名", 38), FORMAT(0,"フォーマット", 46), DEFAULT_VALUE(0,"初期値", 56),
	EDIT_RULE(0,"編集仕様", 70), PROCESSING_RULE(0,"加工式", 80), SELECT_LIST(0,"選択リスト", 97), DISPLAY_CONDITION(0,"表示条件", 107),
	REMARKS(0,"備考", 117), WP_ITEM_NAME(1,"WP項目名", 128);

	private final int level;
	private final String displayName;
	private final int columnIndex;

	ScreenItemDescriptionHeaderEnum(int level, String displayName, int columnIndex) {
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