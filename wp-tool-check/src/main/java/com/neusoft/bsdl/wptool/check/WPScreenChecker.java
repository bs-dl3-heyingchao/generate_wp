package com.neusoft.bsdl.wptool.check;

import com.neusoft.bsdl.wptool.core.exception.WPException;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WPScreenChecker {
    private WPCheckerContext context;

    public WPScreenChecker(WPCheckerContext context) {
        this.context = context;
    }

    public void checkScreenData(ScreenExcelContent screenExcelContent) throws WPException {

    }
}
