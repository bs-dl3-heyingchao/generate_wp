package com.neusoft.bsdl.wptool.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 部分入出力
 */
@Data
@AllArgsConstructor
public class ScreenDefinitionPartInOut {
	/**部分入出力コード*/
	private String partCode;
	/**部分入出力名称*/
	private String partName;
	/**部分入出力オペレーション*/
	private String partOperation;
}
