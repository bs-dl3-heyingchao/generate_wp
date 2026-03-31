package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 画面定義書の解析内容
 */
@Data
public class ScreenDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
	/** 対象データモデル */
	private List<ScreenDefinitionTargetData> targetModels;
	/** 処理対象データモデル */
	private List<ScreenDefinitionProcessingTarget> processingModels;
	/** 入出力タイプ */
	private String ioType;
	/** 対象条件 */
	private String targetCondition;
	/** 外部ファイル */
	private Map<String, String> externalFiles;
	/** 部分入出力 */
	private List<ScreenDefinitionPartInOut> inOutParts;
}