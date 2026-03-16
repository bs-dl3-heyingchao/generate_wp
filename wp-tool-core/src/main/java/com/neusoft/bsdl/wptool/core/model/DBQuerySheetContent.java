package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import lombok.Data;

@Data
public class DBQuerySheetContent {
	/**シート名称*/
	private String sheetName;
	/**テーブル名称*/
	private String tableName;
	/**テーブルID*/
	private String tableId;
	/**クエリーエンティティ*/
	private List<DBQueryEntity> queryEntities;
	/**概要*/
	private DBQuerySummary summary;
	/**dbQuery：検索条件*/
	private String queryCondition;
	/**dbQueryAggregate：集計関数の検索条件*/
	private String queryAggregateCondition;
}
