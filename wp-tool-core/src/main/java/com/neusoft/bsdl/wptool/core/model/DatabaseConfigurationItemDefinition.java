package com.neusoft.bsdl.wptool.core.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * CSVレイアウトのコラム定義
 */
@Data
public class DatabaseConfigurationItemDefinition {
	private String dataModel;      // データモデル
    private String operation;      // 操作
    private String processContent; // 処理内容
    private List<DbConfigItem> items = new ArrayList<>();
}
