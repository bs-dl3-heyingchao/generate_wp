package com.neusoft.bsdl.wptool.core.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 画面定義書の解析内容
 */
@Data
public class ScreenDefinition {
	/** 対象データモデル */
	private List<ScreenDefinitionTargetData> targetModels;
	/** 処理対象データモデル */
	private List<ScreenDefinitionProcessingTarget> processingModels;
	/** 入出力タイプ */
	private String ioType;
	/** 対象条件 */
	private String targetCondition;
	/** 外部ファイル */
	private Map<String,String> externalFiles;
	/**TODO:部分入出力*/
}
