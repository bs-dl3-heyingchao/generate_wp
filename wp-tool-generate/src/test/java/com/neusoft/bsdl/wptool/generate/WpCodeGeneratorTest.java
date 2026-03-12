package com.neusoft.bsdl.wptool.generate;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;
import com.neusoft.bsdl.wptool.validator.WPScreenValidator;

public class WpCodeGeneratorTest {

    public static void main(String[] args) throws Exception {
        FileSource source = new LocalFileSource(args[0]);
        WPContext context = WPContext.create();
        WPScreenValidator screenChecker = new WPScreenValidator(context);
        ScreenExcelContent screenExcelContent = ParseExcelUtils.parseScreenExcel(source);
        screenChecker.validateParseContent(screenExcelContent);
    }

}
