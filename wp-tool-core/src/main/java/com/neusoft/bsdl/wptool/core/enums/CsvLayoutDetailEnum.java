package com.neusoft.bsdl.wptool.core.enums;

/**
 * CSVレイアウトの一覧のヘッダ列挙
 */
public enum CsvLayoutDetailEnum implements HeaderEnum {
	ITEM_NO(0,"項番", 0), FIELD_NAME(1,"項目名", 1), VALIDATION_NAME(1,"表示", 11), ATTRIBUTE_WP(1,"属性(WP)", 13), ATTRIBUTE(1,"属性", 16),
	LENGTH_WP(1,"桁数(WP)", 19), LENGTH(1,"桁数", 22), FIXED_VALUE(1,"固定値", 25), SORTED(0,"表示順", 34), TABLE_NAME(1,"テーブル名", 36),
	ITEM_NAME(1,"項目名", 46),
	REMARKS(0,"備考", 56);
	private final int level;
	private final String displayName;
	private final int columnIndex;

	CsvLayoutDetailEnum(int level,String displayName, int columnIndex) {
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
		return this.level;
	}
}