package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import lombok.Data;

@Data
public class ProcessingFuncSpecificationBtnOperation {
	/** 「XXX」ボタン押下処理 */
	private String opertionName;
	
	/** 1.メッセージ */
	private List<ProcessingFuncSpecificationBtnOperationMsg> messages;

	/** 2.次画面遷移 */
	private ProcessingFuncSpecificationBtnOperationForward screenFoward;
}
