package com.neusoft.bsdl.wptool.core.model;

import lombok.Data;

@Data
public class ProcessingFuncSpecificationBtnOperationMsg {
	/** メッセージの名称 */
	private String messageName;
	/** メッセージのID */
	private String messageId;
	/** メッセージのコンテンツ */
	private String messageContents;
}
