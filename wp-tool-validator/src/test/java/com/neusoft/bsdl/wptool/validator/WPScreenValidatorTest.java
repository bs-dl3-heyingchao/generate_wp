package com.neusoft.bsdl.wptool.validator;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.DBQueryExcelContent;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;

class WPScreenValidatorTest {

    public static void main(String[] args) throws Exception {
        FileSource source = new LocalFileSource(args[0]);
        WPContext context = WPContext.create();
        DBQueryExcelContent queryExcelContent = ParseExcelUtils.parseDBQueryExcel(source);
        WPScreenValidator screenChecker = new WPScreenValidator(context,queryExcelContent.getQuerySheetContents());
        ScreenExcelContent screenExcelContent = ParseExcelUtils.parseScreenExcel(source);
        screenChecker.validateParseContent(screenExcelContent,queryExcelContent.getQuerySheetContents());
    }

}
