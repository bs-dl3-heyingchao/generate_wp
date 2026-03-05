package com.neusoft.bsdl.wptool.core.enums;

/**
 * DB設定項目定義のヘッダ列挙
 */
public enum DBConfigItemDefinitionEnum implements HeaderEnum {
	FUNCTION_NAME(0,"データモデル", 0), FILE_ID(0,"操作", 31),
	FILE_NAMING_RULE(1,"操作コード", 0), CHARACTER_ENCODING(1,"名前", 31);
	private final int level;
	private final String displayName;
	private final int columnIndex;

	DBConfigItemDefinitionEnum(int level,String displayName, int columnIndex) {
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