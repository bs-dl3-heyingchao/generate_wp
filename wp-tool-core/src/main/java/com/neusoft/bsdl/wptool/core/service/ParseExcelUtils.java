package com.neusoft.bsdl.wptool.core.service;

import java.util.List;

import com.neusoft.bsdl.wptool.core.ScreenSheetNameEnum;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ParseExcelContent;
import com.neusoft.bsdl.wptool.core.model.ScreenFuncSpecification;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescription;

public class ParseExcelUtils {
	/**
	 * 
	 * @param source
	 * @return
	 * @throws Exception 
	 */
	public static ParseExcelContent parseExcel(FileSource source) throws Exception {
		ParseExcelContent parseExcelContent = new ParseExcelContent();
		ScreenItemDescriptionParseExcel service = new ScreenItemDescriptionParseExcel();

        List<ScreenItemDescription> fields = service.parseSpecSheet(source, ScreenSheetNameEnum.SCREEN_FIELD.getSheetName());
         System.out.println("excel.size:"+fields.size()); 
        for (ScreenItemDescription field : fields) {
            System.out.println(field.getItemNo() + " | " + field.getFieldName());
        }
        System.out.println("-----------------------------------"); 
        ScreenFuncSpecificationParseExcel service1 = new ScreenFuncSpecificationParseExcel();

        List<ScreenFuncSpecification> fields1 = service1.parseSpecSheet(source, ScreenSheetNameEnum.SCREEN_FUNCTION.getSheetName());
         System.out.println("excel.size:"+fields1.size()); 
         
        for (ScreenFuncSpecification field : fields1) {
            System.out.println(field.getItemNo() + " | " + field.getFunctionName());
        }
        parseExcelContent.setScreenItemDesList(fields);
        return parseExcelContent;
	}
}
