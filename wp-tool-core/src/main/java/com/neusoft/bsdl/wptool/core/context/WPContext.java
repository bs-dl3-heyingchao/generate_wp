package com.neusoft.bsdl.wptool.core.context;

import com.neusoft.bsdl.wptool.core.service.IWPMessageLoaderService;
import com.neusoft.bsdl.wptool.core.service.IWPTableSearchService;
import com.neusoft.bsdl.wptool.core.service.impl.WPMessageLoaderService;
import com.neusoft.bsdl.wptool.core.service.impl.WPTableSearchService;

public class WPContext {
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

	protected WPContext(IWPTableSearchService tableSearchService, IWPMessageLoaderService messageLoaderService) {
		this.tableSearchService = tableSearchService;
		this.messageLoaderService = messageLoaderService;
	}

	public static WPContext create() {
		WPTableSearchService tableSearchService = new WPTableSearchService();
		tableSearchService.initialize();
		WPMessageLoaderService messageLoaderService = new WPMessageLoaderService();
		messageLoaderService.initialize();
		return new WPContext(tableSearchService, messageLoaderService);
	}

}
