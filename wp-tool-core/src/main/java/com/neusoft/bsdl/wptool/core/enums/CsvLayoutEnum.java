package com.neusoft.bsdl.wptool.core.enums;

/**
 * CSVレイアウトのヘッダ列挙
 */
public enum CsvLayoutEnum implements HeaderEnum {
	FUNCTION_NAME(0,"機能名称", 0), FILE_ID(0,"ファイルID", 14), FILE_NAME(0,"ファイル名", 27), INPUT_OUTPUT_TYPE(0,"入出力種別", 40), FILE_FORMAT(0,"ファイル形式", 51),
	FILE_NAMING_RULE(1,"ファイル名規則", 0), CHARACTER_ENCODING(1,"文字コード", 40), LINE_ENCODING(1,"改行コード", 51),
	ITEM_NAME(2,"特記事項", 0);
	private final int level;
	private final String displayName;
	private final int columnIndex;

	CsvLayoutEnum(int level,String displayName, int columnIndex) {
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