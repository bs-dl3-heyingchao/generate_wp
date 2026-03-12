package com.neusoft.bsdl.wptool.core.service.impl;

import com.neusoft.bsdl.wptool.core.model.MessageDefinition;
import com.neusoft.bsdl.wptool.core.service.IWPMessageLoaderService;

public class WPMessageLoaderService implements IWPMessageLoaderService {

	public WPMessageLoaderService() {
	}

	public synchronized void initialize() {
		// TODO:
	}

	@Override
	public MessageDefinition findMessageById(String messageId) {
		// TODO Auto-generated method stub
		MessageDefinition messageDefinition = new MessageDefinition();
		messageDefinition.setMessageId(messageId);
		messageDefinition.setMessageText("{1}は存在しません。");
		messageDefinition.setSeverity("ERROR");
		messageDefinition.setInitFocus("OK");
		messageDefinition.setType("画面メッセージ");
		return messageDefinition;
	}
}
