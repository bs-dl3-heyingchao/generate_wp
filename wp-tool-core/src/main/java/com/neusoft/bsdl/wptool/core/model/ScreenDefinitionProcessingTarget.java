package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScreenDefinitionProcessingTarget implements Serializable {
	/**論理名称*/
	private String logicalName;
	/**物理名称*/
    private String physicalName;
    /**CRUD*/
    private String crud;
	private static final long serialVersionUID = 1L;
}
