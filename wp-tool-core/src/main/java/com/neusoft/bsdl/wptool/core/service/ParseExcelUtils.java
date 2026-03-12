package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.neusoft.bsdl.wptool.core.CommonConstant;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.CsvLayout;
import com.neusoft.bsdl.wptool.core.model.DBConfigDefinition;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.model.ScreenDefinition;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.model.ScreenFuncSpecification;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescriptionResult;
import com.neusoft.bsdl.wptool.core.model.ScreenMetadata;
import com.neusoft.bsdl.wptool.core.model.ScreenValidation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParseExcelUtils {
//    /**
//     * excel解析
//     * 
//     * @param source リソースファイル
//     * @return
//     * @throws Exception
//     */
//    public static DBQueryExcelContent parseDBQueryExcel(FileSource source) throws Exception {
//        
//        
//    }

	/**
	 * excel解析
	 * 
	 * @param source リソースファイル
	 * @return
	 * @throws Exception
	 */
	public static ScreenExcelContent parseScreenExcel(FileSource source) throws Exception {
		log.info("解析対象シート名称リスト:{}", Arrays.asList(CommonConstant.SCREEN_DEFINITION_SHEET.SHEET_NAME,
				CommonConstant.SCREEN_ITEM_DESCRIPTION_SHEET.SHEET_NAME,
				CommonConstant.SCREEN_FUNC_SPECIFICATION_SHEET.SHEET_NAME,
				CommonConstant.SCREEN_VALIDATION_SHEET.SHEET_NAME,
				CommonConstant.CSV_LAYOUT_SHEET.SHEET_NAME, CommonConstant.DB_CONFIG_SHEET.SHEET_NAME));
		List<String> sheetNames = getSheetNames(source.getInputStream());
		// シートリストが存在しない場合、異常終了
		if (sheetNames.isEmpty()) {
			throw new WPParseExcelException(MessageService.getMessage("error.sheets.not.exists"));
		}
		ScreenExcelContent parseExcelContent = new ScreenExcelContent();
		// エラー結果
		List<ExcelParseError> errors = Lists.newArrayList();
		// ヘッダ情報の解析
		ScreenMetadata screenMetadata = ScreenMetadataParser.parseHeaderMetadata(source,
				CommonConstant.MODIFY_HISTORY_SHEET.SHEET_NAME, errors);

		// シートコンテンツの解析
		List<ExcelSheetContent<?>> sheetList = Lists.newArrayList();

		for (String sheetName : sheetNames) {
			if (sheetName.equals(CommonConstant.SCREEN_DEFINITION_SHEET.SHEET_NAME)) {
				// 画面定義書
				ScreenDefinitionParseExcel parseExcel = new ScreenDefinitionParseExcel();
				ScreenDefinition contents = parseExcel.parseSpecSheet(source, sheetName, errors);
				ExcelSheetContent<ScreenDefinition> excelSheetContent = new ExcelSheetContent<>();
				excelSheetContent.setSheetName(sheetName);
				excelSheetContent.setContent(contents);
				sheetList.add(excelSheetContent);
			} else if (sheetName.indexOf(CommonConstant.SCREEN_ITEM_DESCRIPTION_SHEET.SHEET_NAME) != -1) {
				// 画面項目説明書シート(複数シートが存在する可能)
				ScreenItemDescriptionParseExcel parseExcel = new ScreenItemDescriptionParseExcel();
				List<ScreenItemDescriptionResult> contents = parseExcel.parseSpecSheet(source, sheetName, errors);
				ExcelSheetContent<List<ScreenItemDescriptionResult>> excelSheetContent = new ExcelSheetContent<>();
				excelSheetContent.setSheetName(sheetName);
				excelSheetContent.setContent(contents);
				sheetList.add(excelSheetContent);
			} else if (sheetName.equals(CommonConstant.SCREEN_FUNC_SPECIFICATION_SHEET.SHEET_NAME)) {
				// 画面機能定義書
				ScreenFuncSpecificationParseExcel parseExcel = new ScreenFuncSpecificationParseExcel();
				List<ScreenFuncSpecification> contents = parseExcel.parseSpecSheet(source, sheetName, errors);
				ExcelSheetContent<List<ScreenFuncSpecification>> excelSheetContent = new ExcelSheetContent<>();
				excelSheetContent.setSheetName(sheetName);
				excelSheetContent.setContent(contents);
				sheetList.add(excelSheetContent);
			} else if (sheetName.equals(CommonConstant.SCREEN_VALIDATION_SHEET.SHEET_NAME)) {
				// 画面チェック仕様書
				ScreenValidationParseExcel parseExcel = new ScreenValidationParseExcel();
				List<ScreenValidation> contents = parseExcel.parseSpecSheet(source, sheetName, errors);
				ExcelSheetContent<List<ScreenValidation>> excelSheetContent = new ExcelSheetContent<>();
				excelSheetContent.setSheetName(sheetName);
				excelSheetContent.setContent(contents);
				sheetList.add(excelSheetContent);
			} else if (sheetName.equals(CommonConstant.CSV_LAYOUT_SHEET.SHEET_NAME)) {
				// CSVレイアウト
				CsvLayoutParseExcel parseExcel = new CsvLayoutParseExcel();
				CsvLayout contents = parseExcel.parseSpecSheet(source, sheetName, errors);
				ExcelSheetContent<CsvLayout> excelSheetContent = new ExcelSheetContent<>();
				excelSheetContent.setSheetName(sheetName);
				excelSheetContent.setContent(contents);
				sheetList.add(excelSheetContent);
			} else if (sheetName.indexOf(CommonConstant.DB_CONFIG_SHEET.SHEET_NAME) != -1) {
				// DB設定項目定義(複数シートが存在する可能)
				DbConfigItemDefinitionParseExcel parseExcel = new DbConfigItemDefinitionParseExcel();
				DBConfigDefinition contents = parseExcel.parseSpecSheet(source, sheetName, errors);
				ExcelSheetContent<DBConfigDefinition> excelSheetContent = new ExcelSheetContent<>();
				excelSheetContent.setSheetName(sheetName);
				excelSheetContent.setContent(contents);
				sheetList.add(excelSheetContent);
			}

		}
		// エラーが存在の場合、異常終了
		if (!CollectionUtils.isEmpty(errors)) {
			throw new WPParseExcelException(errors);
		}
		// 解析したヘッダ情報をコピーして結果に設定する
		BeanUtils.copyProperties(parseExcelContent, screenMetadata);
		// シートごとに解析したヘッダ情報を結果に設定する
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
