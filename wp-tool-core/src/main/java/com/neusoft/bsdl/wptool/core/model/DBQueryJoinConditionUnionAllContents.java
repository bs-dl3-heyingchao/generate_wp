package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class DBQueryJoinConditionUnionAllContents implements Serializable {
	private static final long serialVersionUID = 1L;
	/** クエリ名称 */
	private String queryName;
	
	/** クエリ */
	private List<DBQueryJoinConditionContents> joinConditions;

	/** 絞込条件 */
	private String condition;

	/** ソート条件 */
	private String sort;
}
