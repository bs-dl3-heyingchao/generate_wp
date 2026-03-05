package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.neusoft.bsdl.wptool.core.ScreenSheetNameEnum;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.CsvLayout;
import com.neusoft.bsdl.wptool.core.model.DBConfigItemDefinition;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.model.ScreenFuncSpecification;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescriptionResult;
import com.neusoft.bsdl.wptool.core.model.ScreenMetadata;
import com.neusoft.bsdl.wptool.core.model.ScreenValidation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParseExcelUtils {
	/**
	 * excel解析
	 * 
	 * @param source リソースファイル
	 * @return
	 * @throws Exception
	 */
	public static ScreenExcelContent parseScreenExcel(FileSource source) throws Exception {
		List<String> sheetNames = getSheetNames(source.getInputStream());
		log.info("sheetLists:{}", sheetNames.toString());
		if (sheetNames.isEmpty()) {
			throw new Exception("シートがないため、無効の仕様書です。。。。。。。");
		}
		ScreenExcelContent parseExcelContent = new ScreenExcelContent();
		// ヘッダ情報の解析
		ScreenMetadata screenMetadata = ScreenMetadataParser.readHeaderMetadata(source, ScreenSheetNameEnum.MODIFY_HISTORY.getSheetName());
		BeanUtils.copyProperties(parseExcelContent, screenMetadata);
		// シートコンテンツの解析
		List<ExcelSheetContent<?>> sheetList = Lists.newArrayList();
		for (String sheetName : sheetNames) {
			if (sheetName.indexOf(ScreenSheetNameEnum.SCREEN_FIELD.getSheetName()) != -1) {
				// 画面項目説明書シート(複数シートが存在する可能)
				ScreenItemDescriptionParseExcel parseExcel = new ScreenItemDescriptionParseExcel();
				List<ScreenItemDescriptionResult> contents = parseExcel.parseSpecSheet(source, sheetName);
				ExcelSheetContent<List<ScreenItemDescriptionResult>> excelSheetContent = new ExcelSheetContent<>();
				excelSheetContent.setSheetName(sheetName);
				excelSheetContent.setContent(contents);
				sheetList.add(excelSheetContent);
			} else if (sheetName.equals(ScreenSheetNameEnum.SCREEN_FUNCTION.getSheetName())) {
				// 画面機能定義書
				ScreenFuncSpecificationParseExcel parseExcel = new ScreenFuncSpecificationParseExcel();
				List<ScreenFuncSpecification> contents = parseExcel.parseSpecSheet(source, sheetName);
				ExcelSheetContent<List<ScreenFuncSpecification>> excelSheetContent = new ExcelSheetContent<>();
				excelSheetContent.setSheetName(sheetName);
				excelSheetContent.setContent(contents);
				sheetList.add(excelSheetContent);
			} else if (sheetName.equals(ScreenSheetNameEnum.SCREEN_VALIDATION.getSheetName())) {
				// 画面チェック仕様書
				ScreenValidationParseExcel parseExcel = new ScreenValidationParseExcel();
				List<ScreenValidation> contents = parseExcel.parseSpecSheet(source, sheetName);
				ExcelSheetContent<List<ScreenValidation>> excelSheetContent = new ExcelSheetContent<>();
				excelSheetContent.setSheetName(sheetName);
				excelSheetContent.setContent(contents);
				sheetList.add(excelSheetContent);
			} else if (sheetName.equals(ScreenSheetNameEnum.CSV_LAYOUT.getSheetName())) {
				// CSVレイアウト
				CsvLayoutParseExcel parseExcel = new CsvLayoutParseExcel();
				CsvLayout contents = parseExcel.parseSpecSheet(source, sheetName);
				ExcelSheetContent<CsvLayout> excelSheetContent = new ExcelSheetContent<>();
				excelSheetContent.setSheetName(sheetName);
				excelSheetContent.setContent(contents);
				sheetList.add(excelSheetContent);
			} else if (sheetName.indexOf(ScreenSheetNameEnum.DB_CONFIG.getSheetName()) != -1) {
				// DB設定項目定義(複数シートが存在する可能)
				DbConfigItemDefinitionParseExcel parseExcel = new DbConfigItemDefinitionParseExcel();
				DBConfigItemDefinition contents = parseExcel.parseSpecSheet(source, sheetName);
				ExcelSheetContent<DBConfigItemDefinition> excelSheetContent = new ExcelSheetContent<>();
				excelSheetContent.setSheetName(sheetName);
				excelSheetContent.setContent(contents);
				sheetList.add(excelSheetContent);
			}
			
		}
		parseExcelContent.setSheetList(sheetList);
		return parseExcelContent;
	}

	/**
	 * シート名称の取得
	 * 
	 * @param inputStream
	 * @return シート名称配列
	 * @throws Exception
	 */
	public static List<String> getSheetNames(InputStream inputStream) throws Exception {
		try (Workbook workbook = WorkbookFactory.create(inputStream)) {
			List<String> sheetNames = Lists.newArrayList();
			int numberOfSheets = workbook.getNumberOfSheets();
			for (int i = 0; i < numberOfSheets; i++) {
				sheetNames.add(workbook.getSheetName(i));
			}
			return sheetNames;
		}
	}
}
