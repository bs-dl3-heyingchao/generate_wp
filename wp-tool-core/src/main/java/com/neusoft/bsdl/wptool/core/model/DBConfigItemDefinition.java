package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.compress.utils.Lists;

import lombok.Data;

/**
 * DB設定項目定義の解析内容
 */
@Data
public class DBConfigItemDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
	//データモデル
	private String dataModel; 
	// 操作
    private String operation;   
    // 操作コード
    private String operationCode;
    // 名前
    private String tableName; 
    //一覧表示
    private List<DBConfigSubItemDefinition> details = Lists.newArrayList();
}
