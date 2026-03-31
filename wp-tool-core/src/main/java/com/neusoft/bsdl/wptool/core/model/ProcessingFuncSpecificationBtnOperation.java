package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class ProcessingFuncSpecificationBtnOperation implements Serializable {
	private static final long serialVersionUID = 1L;
	/** 「XXX」ボタン押下処理 */
	private String opertionName;
	
	/** 1.メッセージ */
	private List<ProcessingFuncSpecificationBtnOperationMsg> messages;

	/** 2.次画面遷移 */
	private ProcessingFuncSpecificationBtnOperationForward screenFoward;
}
