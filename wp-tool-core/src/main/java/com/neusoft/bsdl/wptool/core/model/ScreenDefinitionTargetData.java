package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScreenDefinitionTargetData implements Serializable {
	private static final long serialVersionUID = 1L;
	/**論理名称*/
	private String logicalName;
	/**物理名称*/
    private String physicalName;
}
