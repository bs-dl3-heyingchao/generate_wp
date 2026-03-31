package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class ProcessingFuncSpecification implements Serializable {
	private static final long serialVersionUID = 1L;
	/**1.パラメータ*/
	private List<ProcessingFuncSpecificationParam> params;
	
	/**「XXX」ボタン押下処理*/
	private List<ProcessingFuncSpecificationBtnOperation> btnOpertions;
}
