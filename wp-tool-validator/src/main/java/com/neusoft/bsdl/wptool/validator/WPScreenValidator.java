package com.neusoft.bsdl.wptool.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.neusoft.bsdl.wptool.core.exception.WPCheckException;
import com.neusoft.bsdl.wptool.core.model.DBConfigDefinition;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescriptionResult;
import com.neusoft.bsdl.wptool.core.model.ScreenValidation;
import com.neusoft.bsdl.wptool.core.service.MessageService;
import com.neusoft.bsdl.wptool.validator.CommonConstant.SCREEN_ITEM_DESCRIPTION_SHEET;
import com.neusoft.bsdl.wptool.validator.context.WPValidatorContext;
import com.neusoft.bsdl.wptool.validator.enums.ItemDescriptionIOEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WPScreenValidator {
    private WPValidatorContext context;

    public WPScreenValidator(WPValidatorContext context) {
        this.context = context;
    }
    
    /**
     * 仕様書の内容のバリデーションチェックを実施する
     * @param screenExcelContent
     * @throws WPCheckException
     */
    public void validateParseContent(ScreenExcelContent screenExcelContent) throws WPCheckException {
    	List<String> errors = new ArrayList<>();
    	for (ExcelSheetContent<?> sheet : screenExcelContent.getSheetList()) {
    	    String sheetName = sheet.getSheetName();
    	    if (CommonConstant.PARSE_SHEET_NAME.SCREEN_DEFINITION_SHEET.equals(sheetName)) {
    	        //画面定義書
    	    } else if (CommonConstant.PARSE_SHEET_NAME.SCREEN_ITEM_DESCRIPTION_SHEET.equals(sheetName)) {
    	    	//画面項目説明書
    	    	List<ScreenItemDescriptionResult> validList = (List<ScreenItemDescriptionResult>)sheet.getContent();
    	    	validateScreenItemDescription(validList, errors);
    	    }else if (CommonConstant.PARSE_SHEET_NAME.SCREEN_VALIDATION_SHEET.equals(sheetName)) {
       	     	//画面バリデーション定義書
    	    	List<ScreenValidation> validList = (List<ScreenValidation>)sheet.getContent();
    	    	
       	    }else if (CommonConstant.PARSE_SHEET_NAME.BP_SHEET.equals(sheetName)) {
       	    	//BP定義書
       	    	
       	    }else if (CommonConstant.PARSE_SHEET_NAME.DB_CONFIG_SHEET.equals(sheetName)) {
       	    	//DB設定定義書
       	    	DBConfigDefinition validObj = (DBConfigDefinition)sheet.getContent();
       	    	
       	    	
       	    }
    	}
    	//	エラーが存在する場合、例外をスローする
    	if(!CollectionUtils.isEmpty(errors)) {
			throw new WPCheckException(errors);
    	}
    }
    
    /**
     * 画面項目説明書のバリデーションチェックを実施する
     * @param validList
     * @param errors
     */
	private void validateScreenItemDescription(List<ScreenItemDescriptionResult> validList, List<String> errors) {
		for(ScreenItemDescriptionResult screenItemDescription : validList) {
			screenItemDescription.getItems().forEach(item -> {
				//画面項目説明書の「IO」列には「I（入力）」「O（出力）」「A（アクション）」「G（グループ）」「IO（入出力）」以外の値を記載することはできません
				if(!ItemDescriptionIOEnum.getAllDisplayNames().contains(item.getIo())) {
					errors.add(MessageService.getMessage("error.screen.item.description.io.exists")
							.replace("{0}", item.getItemNo()));
				}
				//[属性(WP)／桁数(WP)]:
				//I/O列の値はAアクション、Gグループの項目の場合、「-」を記載していること。
				if((ItemDescriptionIOEnum.ACTION.getDisplayName().equals(item.getIo()) || 
						ItemDescriptionIOEnum.GROUP.getDisplayName().equals(item.getIo())) &&
						!Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.STR_HAIHUN).contains(item.getAttributeWP()) && 
								!Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.STR_HAIHUN).contains(item.getLengthWP())){
					errors.add(MessageService.getMessage("error.screen.item.description.wp.exists")
							.replace("{0}", item.getItemNo()));
				}
				
				//[属性(WP)／桁数(WP)]:
				//I/O列の値はIO入出力、O出力項目の場合、対象データモデル情報が記載されている場合、「DM」を記載していること。
				//対象データモデル情報が記載されていない場合は、属性、桁数と同じ値を記載していること。
			});
			
		}		
	}
}

