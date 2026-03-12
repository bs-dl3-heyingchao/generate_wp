package com.neusoft.bsdl.wptool.validator.context;

import com.neusoft.bsdl.wptool.validator.service.IWPMessageLoaderService;
import com.neusoft.bsdl.wptool.validator.service.IWPTableSearchService;
import com.neusoft.bsdl.wptool.validator.service.impl.WPMessageLoaderService;
import com.neusoft.bsdl.wptool.validator.service.impl.WPTableSearchService;

public class WPValidatorContext {
	// テーブル/フィールド定義検索サービス
	private IWPTableSearchService tableSearchService;

	// メッセージ定義検索サービス
	private IWPMessageLoaderService messageLoaderService;

	public IWPTableSearchService getTableSearchService() {
		return tableSearchService;
	}

	public IWPMessageLoaderService getMessageLoaderService() {
		return messageLoaderService;
	}

	private WPValidatorContext(IWPTableSearchService tableSearchService, IWPMessageLoaderService messageLoaderService) {
		this.tableSearchService = tableSearchService;
		this.messageLoaderService = messageLoaderService;
	}

	public static WPValidatorContext create() {
		WPTableSearchService tableSearchService = new WPTableSearchService();
		tableSearchService.initialize();
		WPMessageLoaderService messageLoaderService = new WPMessageLoaderService();
		messageLoaderService.initialize();
		return new WPValidatorContext(tableSearchService, messageLoaderService);
	}

}
