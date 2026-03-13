package com.neusoft.bsdl.wptool.core.model;

import lombok.Data;

/**
 * DBQuery定義書の結合条件を保持するクラス
 */
@Data
public class DBQueryJoinCondition {
	/**結合方法*/
	private String method;
	
	/**対象テーブル*/
	private String table;
	
	/**テーブル別名*/
	private String alias;
	
	/**結合条件*/
	private String condition;
}
