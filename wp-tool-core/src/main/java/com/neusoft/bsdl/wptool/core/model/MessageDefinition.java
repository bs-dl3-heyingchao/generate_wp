package com.neusoft.bsdl.wptool.core.model;

import lombok.Data;

/**
 * TODO:メッセージ定義クラス
 */
@Data
public class MessageDefinition {
	/**メッセージID*/
	private String messageId;
	/**メッセージ内容*/
	private String messageText;
	/**レベル: "ERROR","WARNING","INFO"*/
	private String severity; // 
	/**分類: "ログメッセージ","画面メッセージ"*/
	private String type;
	/**初期フォーカス："Yes","No","Cancel","OK"*/
	private String initFocus;
	/**パラメータ*/
	private String param; 
	/**備考*/
	private String remarks; 
}
