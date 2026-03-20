package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import lombok.Data;

@Data
public class DBQueryJoinConditionUnionAllContents {
	/** クエリ */
	private List<DBQueryJoinConditionContents> joinConditions;

	/** 絞込条件 */
	private String condition;

	/** ソート条件 */
	private String sort;
}
