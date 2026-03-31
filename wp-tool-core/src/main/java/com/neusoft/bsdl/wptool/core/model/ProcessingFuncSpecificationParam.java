package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class ProcessingFuncSpecificationParam implements Serializable {
	private static final long serialVersionUID = 1L;
	/**順*/
	private String sort;
	/**項目*/
	private String logicName;
}
