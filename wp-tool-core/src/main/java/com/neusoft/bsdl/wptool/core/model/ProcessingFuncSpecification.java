package com.neusoft.bsdl.wptool.core.model;

import java.util.Map;

import lombok.Data;

@Data
public class ProcessingFuncSpecification {
	/**1.パラメータ⇒Key：順、value:項目*/
	private Map<String,String> params;
}
