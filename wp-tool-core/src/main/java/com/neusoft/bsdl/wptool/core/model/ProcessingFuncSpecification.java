package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import lombok.Data;

@Data
public class ProcessingFuncSpecification {
	/**1.パラメータ*/
	private List<ProcessingFuncSpecificationParam> params;
}
