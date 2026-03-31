package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;

import lombok.Data;

/**
 * DBQuery定義書の概要を保持するクラス
 */
@Data
public class DBQuerySummary implements Serializable {
	private static final long serialVersionUID = 1L;
	/**1．対象テーブル*/
	private String targetTable;
	
	/**TODO:2．結合条件:*/
	private String joinCondition ="TODO:⇒※備考参照";
	
	/**3．補足*/
	private String supplement;
}
