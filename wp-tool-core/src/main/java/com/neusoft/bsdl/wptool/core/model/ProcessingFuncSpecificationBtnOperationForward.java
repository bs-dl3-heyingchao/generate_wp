package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import lombok.Data;

@Data
public class ProcessingFuncSpecificationBtnOperationForward {
	/** 変更破棄確認 */
	private String modifyConfirm;
	/** 次入出力 */
	private String inOut;
	 /** 次入出力のパラメータ（リスト）*/
	private List<ProcessingFuncSpecificationParam> inOutParams;
}
