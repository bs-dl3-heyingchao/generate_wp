package com.neusoft.bsdl.wptool.validator.context;

import com.neusoft.bsdl.wptool.validator.service.IWPTableSearchService;
import com.neusoft.bsdl.wptool.validator.service.impl.WPTableSearchService;

public class WPValidatorContext {
    public IWPTableSearchService getTableSearchService() {
        return tableSearchService;
    }

    private IWPTableSearchService tableSearchService;

    private WPValidatorContext(IWPTableSearchService tableSearchService) {
        this.tableSearchService = tableSearchService;
    }

    public static WPValidatorContext create() {
    	WPTableSearchService tableSearchService = new WPTableSearchService();
        tableSearchService.initialize();
        return new WPValidatorContext(tableSearchService);
    }

}
