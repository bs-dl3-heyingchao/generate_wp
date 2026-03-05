package com.neusoft.bsdl.wptool.core.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.neusoft.bsdl.wptool.core.CommonConstant.DB_CONFIG_SHEET;
import com.neusoft.bsdl.wptool.core.enums.DBConfigItemDefinitionDetailEnum;
import com.neusoft.bsdl.wptool.core.enums.DBConfigItemDefinitionEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.DBConfigItemDefinition;
import com.neusoft.bsdl.wptool.core.model.DBConfigSubItemDefinition;

import lombok.extern.slf4j.Slf4j;

/**
 * DB設定項目定義のコンテンツの解析ツール
 */
@Slf4j
public class DbConfigItemDefinitionParseExcel extends AbstractParseTool {

	public DBConfigItemDefinition parseSpecSheet(FileSource source, String sheetName, List<ExcelParseError> errors)
			throws Exception {
		// エクセルファイルを読込む
		byte[] excelBytes;
		try (InputStream is = source.getInputStream()) {
			excelBytes = is.readAllBytes();
		}

		// バリデーションチェックを実施する
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			validateHeaders(sheet, errors);
			if (!CollectionUtils.isEmpty(errors)) {
				return null;
			}
		}

		// ヘッダ情報を解析する
		DBConfigItemDefinition result;
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			result = readHeaderMetadata(sheet);
		}

		// 明細情報を解析する
		List<DBConfigSubItemDefinition> subLayouts = new ArrayList<>();
		AnalysisEventListener<DBConfigSubItemDefinition> listener = new AnalysisEventListener<DBConfigSubItemDefinition>() {
			@Override
			public void invoke(DBConfigSubItemDefinition row, AnalysisContext context) {
				if (row != null) {
					subLayouts.add(row);
				}
			}

			@Override
			public void doAfterAllAnalysed(AnalysisContext context) {
			}
		};

		try (InputStream bis = new ByteArrayInputStream(excelBytes)) {
			EasyExcel.read(bis, DBConfigSubItemDefinition.class, listener).sheet(sheetName).headRowNumber(9).doRead();
		}

		result.setDetails(subLayouts);
		return result;
	}

	/**
	 * 「DB設定項目定義」シートのヘッダー列、明細列構造のバリデーションチェック
	 * @param sheet シートオブジェクト
	 * @param errors エラーオブジェクト
	 */
	public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
		// DB設定項目定義のヘッダバリデーションチェック
		Row levle0_headerRow = sheet.getRow(DB_CONFIG_SHEET.START_POS_HEADER_INDEX);
		Row levle1_headerRow = sheet.getRow(DB_CONFIG_SHEET.START_POS_HEADER_INDEX + 1);
		for (DBConfigItemDefinitionEnum header : DBConfigItemDefinitionEnum.values()) {
			String expectedName = header.getDisplayName();
			int expectedIndex = header.getColumnIndex();
			String actualName = "";
			if (header.getLevel() == 0) {
				actualName = getCellValue(levle0_headerRow, expectedIndex).trim();
			} else {
				actualName = getCellValue(levle1_headerRow, expectedIndex).trim();
			}

			if (!expectedName.equals(actualName)) {
				errors.add(new ExcelParseError(sheet.getSheetName(), DB_CONFIG_SHEET.START_POS_HEADER_INDEX + 1,
						expectedIndex, MessageService.getMessage("error.format.dbConfig.wrongColumn")));
				break;
			}
		}
		// DB設定項目定義のヘッダバリデーションチェック
		Row detailRow = sheet.getRow(DB_CONFIG_SHEET.START_POS_DETAIL_INDEX);

		for (DBConfigItemDefinitionDetailEnum header : DBConfigItemDefinitionDetailEnum.values()) {
			String expectedName = header.getDisplayName();
			int expectedIndex = header.getColumnIndex();

			String actualName = getCellValue(detailRow, expectedIndex).trim();

			if (!expectedName.equals(actualName)) {
				errors.add(new ExcelParseError(sheet.getSheetName(), DB_CONFIG_SHEET.START_POS_DETAIL_INDEX + 1,
						expectedIndex, MessageService.getMessage("error.format.dbConfig.wrongColumn")));
				break;
			}
		}

	}

	/**
	 * ヘッダ情報取得
	 * 
	 * @param sheet
	 * @return
	 */
	private DBConfigItemDefinition readHeaderMetadata(Sheet sheet) {
		DBConfigItemDefinition layout = new DBConfigItemDefinition();
		// 六行目：データモデル | 操作
		Row row0 = sheet.getRow(DB_CONFIG_SHEET.START_POS_HEADER_INDEX);
		if (row0 != null) {
			layout.setDataModel(getCellValue(row0, 6));
			layout.setOperation(getCellValue(row0, 37));
		}

		// 七行目：操作コード | 名前
		Row row1 = sheet.getRow(DB_CONFIG_SHEET.START_POS_HEADER_INDEX + 1);
		if (row1 != null) {
			layout.setOperationCode(getCellValue(row1, 6));
			layout.setTableName(getCellValue(row1, 37));
		}
		return layout;
	}
}