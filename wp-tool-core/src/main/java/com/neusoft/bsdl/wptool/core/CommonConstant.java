package com.neusoft.bsdl.wptool.core;

/**
 * 共通定数定義
 */
public interface CommonConstant {
	/** セッション項目一覧 */
	interface SESSION_MANAGEMENT_SHEET {
		// 解析列番号
		Integer COL_B = 1;
		Integer COL_C = 2;
		Integer COL_E = 4;
		// 解析説明文字:システム項目
		String STR_SIKAKU = "■";
		String STR_TYPE_SYSTEM_FIELD = "システム項目";
		// シート名称
		String SHEET_NAME = "セッション項目一覧";
	}

	/** dbQuery定義書 */
	interface DBQUERY_SHEET {
		// 解析開始行:ヘッダ
		Integer START_POS_HEADER_INDEX = 5;
		// 解析開始行:一覧
		Integer START_POS_DETAIL_INDEX = 7;
		// 解析開始行:データ
		Integer START_POS_DATA_INDEX = 9;
		// 解析列番号
		Integer COL_A = 0;
		Integer COL_B = 1;
		Integer COL_C = 2;
		Integer COL_D = 3;
		Integer COL_E = 4;
		Integer COL_F = 5;
		Integer COL_G = 6;
		Integer COL_H = 7;
		Integer COL_I = 8;
		Integer COL_J = 9;
		Integer COL_K = 10;
		Integer COL_L = 11;
		Integer COL_M = 12;
		Integer COL_P = 15;
		Integer COL_AB = 27;
		Integer COL_AD = 25;
		Integer COL_AJ = 35;
		Integer COL_AK = 36;

		String STR_TRUE = "TRUE";

		// 解析説明文字
		String STR_CTRL = "\n";
		String STR_METHOD_JUDGMENT_CONTAIN = "結合";
		String STR_SHEET_NAME_MODIFY_HISTORY = "改版履歴";
		String STR_SECTION_SUMMARY = "概要";
		String STR_SECTION_QUERY_CONDITION = "dbQuery：検索条件";
		String STR_SECTION_QUERY_AGGREGATE_CONDITION = "dbQueryAggregate：集計関数の検索条件";
		String STR_JUDGEMENT_JOIN = "・結合条件";
		String STR_JUDGEMENT_PREFIX = "★";
		String STR_JUDGEMENT_QUERY = "クエリ";

		// 1から5までの数字にマッチする正規表現
		String MATCH_FROM_ONE_TO_FIVE = "^[1-5１-５][．.].*";
	}

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
		// 部分入出力：partCode列
		int COL_AI = 34;
		// 部分入出力：partName列
		int COL_AN = 39;
		// 部分入出力：partOperation列
		int COL_BC = 54;
		
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

	interface PROCESSING_FUNCTION_SPECIFICATION_SHEET {
		// シート名称
		String SHEET_NAME = "処理機能記述書";
		// 1.パラメータ
		String SECTION_PARAM = "1.パラメータ";
		// XXXボタン押下処理
		String SECTION_BTN_OPERATION = "ボタン押下処理";
		// "1)メッセージ"
		String SECTION_SUB_MESSAGE = "1)メッセージ";
		// 2)次画面遷移
		String SECTION_SUB_REDIREECT = "2)次画面遷移";
		// 事前メッセージ
		String JIZEN_MESSAGE_ITEM = "事前メッセージ";
		// OKメッセージ
		String OK_MESSAGE_ITEM = "OKメッセージ";
		// NGメッセージ
		String NG_MESSAGE_ITEM = "NGメッセージ";
		// 変更破棄確認
		String MODIFY_CONFIRM = "変更破棄確認";
		// 次入出力
		String IN_OUT = "次入出力";
		// メッセージの区切り
		String MESSAGE_COLON = "：";
		// パラメータ列名称：順
		String STR_COL_SORT = "順";
		// パラメータ列名称：項目
		String STR_COL_ITEM = "項目";
		// JsonNodeの子ノート：nodeKey
		String STR_NODE_NODE_KEY = "nodeKey";
		// JsonNodeの子ノート：childTreeNodes
		String STR_NODE_CHILD_TREE_NODES = "childTreeNodes";
		// JsonNodeの子ノート：value
		String STR_NODE_VALUE = "value";
		// JsonNodeの子ノート：tableList
		String STR_NODE_TABLE_LIST = "tableList";
		//メッセージのコンテンツの正規表現式	
		String PATTERN_MESSAGE="：([A-Z0-9]+)\\s*(.*)";
	}
}
