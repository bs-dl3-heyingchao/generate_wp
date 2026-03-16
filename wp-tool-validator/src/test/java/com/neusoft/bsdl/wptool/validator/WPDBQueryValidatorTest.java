package com.neusoft.bsdl.wptool.validator;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.DBQueryExcelContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;

class WPDBQueryValidatorTest {

    public static void main(String[] args) throws Exception {
        FileSource source = new LocalFileSource(args[0]);
        WPContext context = WPContext.create();
        WPDBQueryValidator screenChecker = new WPDBQueryValidator(context);
        DBQueryExcelContent dbQueryExcelContent = ParseExcelUtils.parseDBQueryExcel(source);
        screenChecker.validateParseContent(dbQueryExcelContent);
    }

}
