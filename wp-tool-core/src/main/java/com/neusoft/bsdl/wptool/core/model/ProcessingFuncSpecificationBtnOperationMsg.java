package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class ProcessingFuncSpecificationBtnOperationMsg implements Serializable {
	private static final long serialVersionUID = 1L;
	/** メッセージの名称 */
	private String messageName;
	/** メッセージのID */
	private String messageId;
	/** メッセージのコンテンツ */
	private String messageContents;
}
