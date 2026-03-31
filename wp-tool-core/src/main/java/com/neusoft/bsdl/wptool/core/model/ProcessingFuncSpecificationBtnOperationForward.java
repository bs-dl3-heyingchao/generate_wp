package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class ProcessingFuncSpecificationBtnOperationForward implements Serializable {
	private static final long serialVersionUID = 1L;
	/** 変更破棄確認 */
	private String modifyConfirm;
	/** 次入出力 */
	private String inOut;
	 /** 次入出力のパラメータ（リスト）*/
	private List<ProcessingFuncSpecificationParam> inOutParams;
}
