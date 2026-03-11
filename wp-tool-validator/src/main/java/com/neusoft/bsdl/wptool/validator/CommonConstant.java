package com.neusoft.bsdl.wptool.validator;

/**
 * 共通定数定義
 */
public interface CommonConstant {
	/** 解析対象シート名称 */
	interface PARSE_SHEET_NAME {
		// 画面定義書
		String SCREEN_DEFINITION_SHEET = "画面定義書";
		// 画面項目説明書
		String SCREEN_ITEM_DESCRIPTION_SHEET = "画面項目説明書";
		// 画面機能定義書
		String SCREEN_FUNC_SPECIFICATION_SHEET = "画面機能定義書";
		// 画面チェック仕様書
		String SCREEN_VALIDATION_SHEET = "画面チェック仕様書";
		// CSVレイアウト
		String CSV_LAYOUT_SHEET = "CSVレイアウト";
		// DB設定項目定義
		String DB_CONFIG_SHEET = "DB設定項目定義";
		// 処理機能記述書
		String BP_SHEET = "処理機能記述書";
	}
	
	interface SCREEN_ITEM_DESCRIPTION_SHEET{
		//文字列：-,ー
		String[] STR_HAIHUN = {"-","ー"};
	}
}
