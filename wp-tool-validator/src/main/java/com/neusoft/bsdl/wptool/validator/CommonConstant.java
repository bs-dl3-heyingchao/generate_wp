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

	/** 画面項目説明書 */
	interface SCREEN_ITEM_DESCRIPTION_SHEET {
		// TODO ： 文字列：-,－
		String[] ARR_HAIHUN = { "-", "－" };
		// 対象データモデル情報が記載されている文字列：-,－,null以外の日本語
		String[] ARR_OUTSIDE_SCOPE = { "-", "－", null, "" };
		// 文字列：DM
		String STR_DM = "DM";
		// 文字列：-
		String STR_HAIHUN = "-";
		// 文字列：降順
		String STR_SORT_DESC = "降順";
		// 文字列：昇順
		String STR_SORT_ASC = "昇順";
		// 文字列：情報一覧
		String STR_SORT_SCOPE_TITLE = "情報一覧";
		// 文字列：数値項目、日付項目
		String[] ARR_DATA_TYPE = { "NUM", "DATE" };
		// 文字列：数値項目、日付項目
		String STR_NO_DISPLAY = "非表示";
		// 文字列：ドロップダウン
		String STR_DROPDOWN = "ドロップダウン";
		//文字列：ラジオボタン
		String STR_RADIO_BUTTON = "ラジオボタン";
		//文字列：複数選択チェックボックス
		String STR_DUPLICATE_CHECKBOX = "複数選択チェックボックス";
	}
}
