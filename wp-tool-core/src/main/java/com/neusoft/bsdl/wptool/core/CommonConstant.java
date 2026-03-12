package com.neusoft.bsdl.wptool.core;

/**
 * 共通定数定義
 */
public interface CommonConstant {
	/** 改版履歴 */
	interface MODIFY_HISTORY_SHEET {
		// シート名称
		String SHEET_NAME = "改版履歴";
		// 解析開始行:ヘッダ
		Integer START_POS_HEADER_INDEX = 0;
		// 解析開始行:ヘッダ
		Integer START_POS_DATA_INDEX = 2;
	}

	/** 画面定義書 */
	interface SCREEN_DEFINITION_SHEET {
		// シート名称
		String SHEET_NAME = "画面定義書";
		// A列：論理名など
		int COL_A = 0;
		// J列：物理名など
		int COL_J = 9; 
		 // S列：CRUD
		int COL_CRUD = 18;
		// U列：対象条件 / 入出力タイプ
		int COL_U = 20; 
		// BP列：対象条件のスコープの終了列
		int COL_BP = 67; 
		// AB列：外部ファイルの項目名
		int COL_AB = 27; 
		// AG列：外部ファイルの項目値
		int COL_AG = 32; 
		// シート名称
		String STR_JAVASCRIPT = "Javascript";
		// シート名称
		String STR_CSS = "CSS";
	}

	/** 画面項目説明書 */
	interface SCREEN_ITEM_DESCRIPTION_SHEET {
		// シート名称
		String SHEET_NAME = "画面項目説明書";
		// 解析開始行:ヘッダ
		Integer START_POS_HEADER_INDEX = 4;
		// 解析開始行:データ
		Integer START_POS_DATA_INDEX = 6;
	}

	/** 画面機能定義書 */
	interface SCREEN_FUNC_SPECIFICATION_SHEET {
		// シート名称
		String SHEET_NAME = "画面機能定義書";
		// 解析開始行:ヘッダ
		Integer START_POS_HEADER_INDEX = 4;
		// 解析開始行:データ
		Integer START_POS_DATA_INDEX = 6;
	}

	/** 画面チェック仕様書 */
	interface SCREEN_VALIDATION_SHEET {
		// シート名称
		String SHEET_NAME = "画面チェック仕様書";
		// バリエーション要否
		String MARK = "〇";
		// 解析開始行:ヘッダ
		Integer START_POS_HEADER_INDEX = 4;
		// 解析開始行:データ
		Integer START_POS_DATA_INDEX = 6;
		// チェックアクションエリアのスタートインデックス
		Integer ACTION_START_INDEX = 50;
		// チェックアクションエリアの最大の列数
		Integer ACTION_COLUMN_COUNT = 15;
	}

	/** CSVレイアウト */
	interface CSV_LAYOUT_SHEET {
		// シート名称
		String SHEET_NAME = "CSVレイアウト";
		// 解析開始行:ヘッダ
		Integer START_POS_HEADER_INDEX = 4;
		// 解析開始行:一覧
		Integer START_POS_DETAIL_INDEX = 7;
		// 解析開始行:データ
		Integer START_POS_DATA_INDEX = 9;
	}

	/** DB設定項目定義 */
	interface DB_CONFIG_SHEET {
		// シート名称
		String SHEET_NAME = "DB設定項目定義";
		// 解析開始行:ヘッダ
		Integer START_POS_HEADER_INDEX = 5;
		// 解析開始行:一覧
		Integer START_POS_DETAIL_INDEX = 8;
		// 解析開始行:データ
		Integer START_POS_DATA_INDEX = 9;
	}
}
