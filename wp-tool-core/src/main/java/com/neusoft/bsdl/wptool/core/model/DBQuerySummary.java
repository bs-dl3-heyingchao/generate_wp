package com.neusoft.bsdl.wptool.core.model;

import lombok.Data;

/**
 * DBQuery定義書の概要を保持するクラス
 */
@Data
public class DBQuerySummary {
	/**1．対象テーブル*/
	private String targetTable;
	
	/**TODO:2．結合条件:*/
	private String joinCondition ="TODO:⇒※備考参照";
	
	/**3．補足*/
	private String supplement;
}
