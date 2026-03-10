package com.neusoft.bsdl.wptool.check;

import com.neusoft.bsdl.wptool.check.context.WPCheckerContext;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;

class WPScreenCheckerTest {

    public static void main(String[] args) {
        WPCheckerContext context = WPCheckerContext.create();
        WPScreenChecker screenChecker = new WPScreenChecker(context);

        ScreenExcelContent screenExcelContent = new ScreenExcelContent();
        screenChecker.checkScreenData(screenExcelContent);
    }

}
