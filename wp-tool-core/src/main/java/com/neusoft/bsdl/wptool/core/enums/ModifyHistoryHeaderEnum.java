package com.neusoft.bsdl.wptool.core.enums;

/**
 * 改版履歴のヘッダ情報列挙
 */
public enum ModifyHistoryHeaderEnum implements HeaderEnum{
	SYSTEM("システム", 0), SUB_SYSTEM("サブシステム", 7), PHASE("フェーズ", 14), DOCUMENT_NAME("ドキュメント名", 22),
	FUNCTION_TYPE("機能分類", 31), FUNCTION_ID("機能ID", 46), SCREEN_ID("画面ID", 53), SCREEN_NAME("画面名", 63);

	private final String displayName;
	private final int columnIndex;

	ModifyHistoryHeaderEnum(String displayName, int columnIndex) {
		this.displayName = displayName;
		this.columnIndex = columnIndex;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getColumnIndex() {
		return columnIndex;
	}
}