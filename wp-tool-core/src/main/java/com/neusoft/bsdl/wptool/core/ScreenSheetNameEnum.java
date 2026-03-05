package com.neusoft.bsdl.wptool.core;

/**
 * 仕様書のシート名称
 */
public enum ScreenSheetNameEnum {
	// 改版履歴
	MODIFY_HISTORY("改版履歴"),
	// 画面項目説明書
	SCREEN_FIELD("画面項目説明書"),
	// 画面機能定義書
	SCREEN_FUNCTION("画面機能定義書"),
	// 画面チェック仕様書
	SCREEN_VALIDATION("画面チェック仕様書"),
	//CSVレイアウト
	CSV_LAYOUT("CSVレイアウト"),
	// DB設定項目定義
	DB_CONFIG("DB設定項目定義_"),
	// 処理機能記述書
	FUNCTION_BUSINESS("処理機能記述書設定");

	private final String sheetName;

	ScreenSheetNameEnum(String sheetName) {
		this.sheetName = sheetName;
	}

	public String getSheetName() {
		return sheetName;
	}

	public static ScreenSheetNameEnum fromSheetName(String name) {
		if (name == null) {
			return null;
		}
		for (ScreenSheetNameEnum type : values()) {
			if (type.sheetName.equals(name.trim())) {
				return type;
			}
		}
		return null;
	}
}