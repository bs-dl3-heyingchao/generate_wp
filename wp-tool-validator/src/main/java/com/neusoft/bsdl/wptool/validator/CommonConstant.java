package com.neusoft.bsdl.wptool.validator;

import java.util.regex.Pattern;

/**
 * 共通定数定義
 */
public interface CommonConstant {
	// エラーメッセージの区切り
	public static final String MESSAGE_KUGIRI = " : ";

	// テーブル定義書のデータ型(WP)列の格納されるKEY
	public static final String STR_WP_TYPE = "WP_TYPE";

	// テーブル定義書のデータ型(WP)列の格納される長さ:PRE
	public static final String STR_WP_LEN_PRE = "WP_LEN_PRE";

	// テーブル定義書のデータ型(WP)列の格納される長さ:S
	public static final String STR_WP_LEN_S = "WP_LEN_S";

	// テーブル定義書のデータ型(WP)列の格納される長さ:PRE:B
	public static final String STR_WP_LEN_B = "WP_LEN_B";

	// キーグループは1の場合、プライマリーキーとする
	public static final String GROUP_KEY = "1";

	/** dbQuery定義書 */
	interface DBQUERY_SHEET {
		// TODO:[テーブル名称] WPネーミング規約にそった名称を記載していること。
		Pattern PATTERN_TABLE_NAME = Pattern.compile(
				"^[\\u4E00-\\u9FFF\\u3040-\\u309F\\u30A0-\\u30FF\\u30FC_]*(_|)[\\u4E00-\\u9FFF\\u3040-\\u309F\\u30A0-\\u30FF\\u30FC_]*$");
		// TODO:[テーブルID] WPネーミング規約にそった名称を記載していること。
		Pattern PATTERN_TABLE_ID = Pattern.compile("^[A-Z]{1}_[A-Z]{3}[0-9]{4}_[0-9]{4}$", Pattern.CASE_INSENSITIVE);

		// 文字列：BOOL
		String STR_BOOL = "BOOL";
	}

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

	/** 画面定義書 */
	interface SCREEN_DEFINITION_SHEET {
		// 対象条件 文字列：エラー
		String NAMED_PARAM = "@NAMEDPARAM";

		// 行末がコンマ（,）、AND、OR であることをマッチ（末尾の空白を無視、複数行対応）
		Pattern INVALID_ENDING = Pattern.compile(".*(?:\\b(?:AND|OR)\\s*|,)\\s*$",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

		// @NAMEDPARAM モード用：文字列中の任意の位置にある AND/OR をマッチ（大文字小文字を区別しない）
		Pattern AND_OR_ANYWHERE = Pattern.compile("\\b(?:AND|OR)\\b", Pattern.CASE_INSENSITIVE);
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
		// 文字列：ラジオボタン
		String STR_RADIO_BUTTON = "ラジオボタン";
		// 文字列：複数選択チェックボックス
		String STR_DUPLICATE_CHECKBOX = "複数選択チェックボックス";
		// 文字列：選択リストの配列
		String[] ARR_SELECT_LIST = { "[DM]", "[条件]", "[値]", "[名称]", "[ソート順]", "[ソートタイプ]" };
	}

	/** 画面チェック仕様書 */
	interface SCREEN_VALIDATION_SHEET {
		// 種別文字列：エラー
		String STR_ERROR = "エラー";
		// 種別文字列：ワーニング
		String STR_WARNING = "ワーニング";
	}

	/** 画面チェック仕様書 */
	interface DB_CONFIG_SHEET {
		// 実際のDBに対しての操作（登録）
		String STR_DB_OPERATION_INSERT_NAME = "登録";
		String STR_DB_OPERATION_INSERT_PREFIX = "I";

		// 実際のDBに対しての操作（更新）
		String STR_DB_OPERATION_UPD_NAME = "更新";
		String STR_DB_OPERATION_UPD_PREFIX = "U";

		// TODO: [操作コード]WPネーミング規約
		// 格式：{9位大写英数字}_{I/U}{3位数字}
		// 登录→ 必须是 _Ixxx
		// 更新→ 必须是 _Uxxx 例：MTI350S01_I010
		Pattern PATTERN_OPERATION_CODE = Pattern.compile("^[A-Z0-9]{9}_([IU])[0-9]{3}$", Pattern.CASE_INSENSITIVE);

		// TODO： [名前]WPネーミング規約 例：汎用テーブル詳細_登録 (日本語と半角アンダーバーを許可)
		Pattern PATTERN_NAME = Pattern.compile(
				"^[\\u4E00-\\u9FFF\\u3040-\\u309F\\u30A0-\\u30FF\\u30FC_]*(_|)[\\u4E00-\\u9FFF\\u3040-\\u309F\\u30A0-\\u30FF\\u30FC_]*$");
	}
}
