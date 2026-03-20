package com.neusoft.bsdl.wptool.core.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * DBQuery定義書の結合条件の備考を保持するクラス
 */
@Data
public class DBQueryJoinCondition {
	/**備考の解析対象はUNION ALLを使用したパターンではないかの判断区分*/
	private boolean isUnionAllCase;
	
	/**・結合条件一覧テーブル*/
	private List<DBQueryJoinConditionContents> normaljoinConditions;
	
	/**セクション対象*/
	private Map<String,String> sectionContents;
	
	/**unionAll結合条件*/
	private List<DBQueryJoinConditionUnionAllContents> unionAlljoinConditions;
}
