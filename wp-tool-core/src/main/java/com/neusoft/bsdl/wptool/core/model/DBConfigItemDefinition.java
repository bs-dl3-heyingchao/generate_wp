package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import lombok.Data;

/**
 * DB設定項目定義のコラム定義
 */
@Data
public class DBConfigItemDefinition {
	//データモデル
	private String dataModel; 
	// 操作
    private String operation;   
    // 操作コード
    private String processCode; 
    // 名前
    private String name; 
    //一覧表示
    private List<DBConfigSubItemDefinition> items = Lists.newArrayList();
}
