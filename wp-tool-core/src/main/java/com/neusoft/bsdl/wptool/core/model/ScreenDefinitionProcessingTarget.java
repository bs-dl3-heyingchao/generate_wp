package com.neusoft.bsdl.wptool.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScreenDefinitionProcessingTarget {
	/**論理名称*/
	private String logicalName;
	/**物理名称*/
    private String physicalName;
    /**CRUD*/
    private String crud;
}
