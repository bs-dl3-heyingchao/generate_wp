package com.neusoft.bsdl.wptool.validator;

import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;
import com.neusoft.bsdl.wptool.validator.context.WPValidatorContext;

class WPScreenValidatorTest {

	public static void main(String[] args) throws Exception {
		FileSource source = new LocalFileSource(args[0]);
		WPValidatorContext context = WPValidatorContext.create();
		WPScreenValidator screenChecker = new WPScreenValidator(context);
		 ScreenExcelContent screenExcelContent = ParseExcelUtils.parseScreenExcel(source);
		screenChecker.validateParseContent(screenExcelContent);
	}

}
