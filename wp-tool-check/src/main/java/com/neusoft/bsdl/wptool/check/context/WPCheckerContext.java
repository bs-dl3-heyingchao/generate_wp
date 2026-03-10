package com.neusoft.bsdl.wptool.check.context;

import com.neusoft.bsdl.wptool.check.service.WPTableSearchService;
import com.neusoft.bsdl.wptool.check.service.impl.WPTableSearchServiceImpl;

public class WPCheckerContext {
    public WPTableSearchService getTableSearchService() {
        return tableSearchService;
    }

    private WPTableSearchService tableSearchService;

    private WPCheckerContext(WPTableSearchService tableSearchService) {
        this.tableSearchService = tableSearchService;
    }

    public static WPCheckerContext create() {
        WPTableSearchServiceImpl tableSearchService = new WPTableSearchServiceImpl();
        tableSearchService.initialize();
        return new WPCheckerContext(tableSearchService);
    }

}
