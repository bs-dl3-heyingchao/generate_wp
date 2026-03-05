package com.neusoft.bsdl.wptool.core.enums;

/**
 * DB設定項目定義の一覧のヘッダ列挙
 */
public enum DBConfigItemDefinitionDetailEnum implements HeaderEnum {
	LOGICAL_NAME("項目", 0),PHYSICAL_NAME("", 11), CONFIG_CONTENT("設定内容", 26),
	REMARKS("備考", 46);

	private final String displayName;
	private final int columnIndex;

	DBConfigItemDefinitionDetailEnum(String displayName, int columnIndex) {
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