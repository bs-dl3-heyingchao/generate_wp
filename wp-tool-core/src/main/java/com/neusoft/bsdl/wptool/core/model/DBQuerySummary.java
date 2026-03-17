package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import lombok.Data;

/**
 * DBQuery定義書の概要を保持するクラス
 */
@Data
public class DBQuerySummary {
	/**１．対象テーブル*/
	private String targetTable;
	
	/**２．対象テーブルの条件*/
	private String targetTableCondition;
	
	/**3．結合条件*/
	private List<DBQueryJoinCondition> joinCondition;
	
	/**4．ソート条件*/
	private String sortCondition;
	
	/**5．補足*/
	private String supplement;
}
