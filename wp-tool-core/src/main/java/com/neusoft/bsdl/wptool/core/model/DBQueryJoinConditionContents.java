package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;

import lombok.Data;

/**
 * DBQuery定義書の結合条件の備考を保持するクラス
 */
@Data
public class DBQueryJoinConditionContents implements Serializable {
	private static final long serialVersionUID = 1L;
	/**テーブル名*/
	private String tableName;
	
	/**テーブル別名*/
	private String alias;
	
	/**結合方式*/
	private String method;
	
	/**結合条件*/
	private String condition;
}
