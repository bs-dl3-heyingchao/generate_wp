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

import com.alibaba.excel.util.StringUtils;
import com.neusoft.bsdl.wptool.core.CommonConstant.DB_CONFIG_SHEET;
import com.neusoft.bsdl.wptool.core.enums.DBConfigItemDefinitionDetailEnum;
import com.neusoft.bsdl.wptool.core.enums.DBConfigItemDefinitionEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.DBConfigDefinition;
import com.neusoft.bsdl.wptool.core.model.DBConfigItemDefinition;
import com.neusoft.bsdl.wptool.core.model.DBConfigSubItemDefinition;

import lombok.extern.slf4j.Slf4j;

/**
 * DB設定項目定義のコンテンツの解析ツール（支持多操作ブロック）
 */
@Slf4j
public class DbConfigItemDefinitionParseExcel extends AbstractParseTool {

	/**
	 * メイン解析メソッド
	 */
	public DBConfigDefinition parseSpecSheet(FileSource source, String sheetName, List<ExcelParseError> errors)
			throws Exception {
		byte[] excelBytes = readExcelBytes(source);

		// ヘッダ情報のバリデーションチェック
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			//バリデーションチェックを実施する
			validateHeaders(sheet, errors);
			if (!errors.isEmpty()) {
				return null;
			}
		}

		// ヘッダ情報が正常な場合、コンテンツの解析を実施
		List<DBConfigItemDefinition> processList = new ArrayList<>();
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			int lastRowNum = sheet.getLastRowNum();
			int currentRow = DB_CONFIG_SHEET.START_POS_HEADER_INDEX;

			while (currentRow <= lastRowNum) {
				Row row = sheet.getRow(currentRow);
				String cell0 = getCellValue(row, 0).trim();
				if (DBConfigItemDefinitionEnum.FUNCTION_NAME.getDisplayName().equals(cell0)) {
					DBConfigItemDefinition item = new DBConfigItemDefinition();
					//ヘッダ情報の設定
					item.setDataModel(getCellValue(row, 6).trim());
					item.setOperation(getCellValue(row, 37).trim());
					Row nextRow = sheet.getRow(currentRow + 1);
					if (nextRow != null) {
						item.setOperationCode(getCellValue(nextRow, 6).trim());
						item.setTableName(getCellValue(nextRow, 37).trim());
					}
					//明細情報の設定
					int detailStartRow = currentRow + 4;
					int nextBlockStart = findNextBlockStart(sheet, detailStartRow);
					int detailEndRow = nextBlockStart - 1;

					List<DBConfigSubItemDefinition> details = readDetailsFromRange(sheet, detailStartRow, detailEndRow);
					item.setDetails(details);
					processList.add(item);

					currentRow = nextBlockStart;
				} else {
					currentRow++;
				}
			}
		}

		DBConfigDefinition result = new DBConfigDefinition();
		result.setProcessList(processList);
		return result;
	}

	/**
	 * 指定した行範囲から明細情報を読み取る
	 * 
	 * @param sheet
	 * @param startRow
	 * @param endRow
	 * @return
	 */
	private List<DBConfigSubItemDefinition> readDetailsFromRange(Sheet sheet, int startRow, int endRow) {
		List<DBConfigSubItemDefinition> details = new ArrayList<>();
		int lastRowNum = sheet.getLastRowNum();
		for (int i = startRow; i <= endRow && i <= lastRowNum; i++) {
			Row row = sheet.getRow(i);
			if (row == null)
				continue;

			String cell0 = getCellValue(row, 0).trim();
			if (DBConfigItemDefinitionEnum.FUNCTION_NAME.getDisplayName().equals(cell0)) {
				break;
			}

			DBConfigSubItemDefinition detail = parseDetailRow(row);
			if (detail != null && !StringUtils.isEmpty(detail.getLogicalName())) {
				details.add(detail);
			}
		}
		return details;
	}

	/**
	 * 解析单行明细
	 */
	private DBConfigSubItemDefinition parseDetailRow(Row row) {
		DBConfigSubItemDefinition detail = new DBConfigSubItemDefinition();

		// 根据 DBConfigItemDefinitionDetailEnum 的列索引映射字段
		for (DBConfigItemDefinitionDetailEnum field : DBConfigItemDefinitionDetailEnum.values()) {
			String value = getCellValue(row, field.getColumnIndex()).trim();
			switch (field) {
			case LOGICAL_NAME:
				detail.setLogicalName(value);
				break;
			case PHYSICAL_NAME:
				detail.setPhysicalName(value);
				break;
			case CONFIG_CONTENT:
				detail.setConfigContent(value);
				break;
			case REMARKS:
				detail.setRemarks(value);
				break;
			default:
				break;
			}
		}
		return detail;
	}

	/**
	 * 次の操作ブロックの開始行を見つける
	 * 
	 * @param sheet
	 * @param fromRow
	 * @return
	 */
	private static int findNextBlockStart(Sheet sheet, int fromRow) {
		int lastRow = sheet.getLastRowNum();
		for (int i = fromRow; i <= lastRow; i++) {
			Row row = sheet.getRow(i);
			if (row != null) {
				String cell0 = getCellValue(row, 0).trim();
				if (DBConfigItemDefinitionEnum.FUNCTION_NAME.getDisplayName().equals(cell0)) {
					return i;
				}
			}
		}
		return lastRow + 1;
	}

	/**
	 * ヘッダ情報バリデーションチェックを実施する
	 * 
	 * @param sheet
	 * @param errors
	 */
	public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
		int currentRow = DB_CONFIG_SHEET.START_POS_HEADER_INDEX;
		int lastRowNum = sheet.getLastRowNum();

		while (currentRow <= lastRowNum) {
			Row row = sheet.getRow(currentRow);
			String cell0 = getCellValue(row, 0).trim();
			if (DBConfigItemDefinitionEnum.FUNCTION_NAME.getDisplayName().equals(cell0)) {
				validateMainHeader(sheet, currentRow, errors);
				// ヘッダ情報のフォーマットが正しくない場合、処理終了
				if (!CollectionUtils.isEmpty(errors)) {
					return;
				}
				int detailHeaderRowNum = currentRow + 3;
				Row detailHeaderRow = sheet.getRow(detailHeaderRowNum);
				if (detailHeaderRow != null) {
					validateDetailHeader(detailHeaderRow, detailHeaderRowNum, sheet.getSheetName(), errors);
				} else {
					errors.add(new ExcelParseError(sheet.getSheetName(), detailHeaderRowNum + 1, 0,
							MessageService.getMessage("error.format.dbConfig.wrongColumn")));
				}
				currentRow = findNextBlockStart(sheet, currentRow + 4);
			} else {
				currentRow++;
			}
		}
	}

	/**
	 * メインヘッダのバリデーションチェックを実施する
	 * 
	 * @param sheet
	 * @param mainHeaderStartRow
	 * @param errors
	 */
	private static void validateMainHeader(Sheet sheet, int mainHeaderStartRow, List<ExcelParseError> errors) {
		Row level0Row = sheet.getRow(mainHeaderStartRow);
		Row level1Row = sheet.getRow(mainHeaderStartRow + 1);

		for (DBConfigItemDefinitionEnum header : DBConfigItemDefinitionEnum.values()) {
			String expected = header.getDisplayName();
			int colIndex = header.getColumnIndex();
			String actual = "";
			Row targetRow = (header.getLevel() == 0) ? level0Row : level1Row;
			if (targetRow != null) {
				actual = getCellValue(targetRow, colIndex).trim();
			}
			if (!expected.equals(actual)) {
				errors.add(new ExcelParseError(sheet.getSheetName(), mainHeaderStartRow + header.getLevel() + 1,
						colIndex + 1, MessageService.getMessage("error.format.dbConfig.wrongColumn")));
			}
		}
	}

	/**
	 * 明細ヘッダのバリデーションチェックを実施する
	 * 
	 * @param detailHeaderRow
	 * @param rowNum
	 * @param sheetName
	 * @param errors
	 */
	private static void validateDetailHeader(Row detailHeaderRow, int rowNum, String sheetName,
			List<ExcelParseError> errors) {
		for (DBConfigItemDefinitionDetailEnum header : DBConfigItemDefinitionDetailEnum.values()) {
			String expected = header.getDisplayName();
			int colIndex = header.getColumnIndex();
			String actual = getCellValue(detailHeaderRow, colIndex).trim();
			if (!expected.equals(actual)) {
				errors.add(new ExcelParseError(sheetName, rowNum + 1, colIndex + 1,
						MessageService.getMessage("error.format.dbConfig.wrongColumn")));
			}
		}
	}

	/**
	 * Excelファイルをバイト配列として読み取る
	 * 
	 * @param source
	 * @return
	 * @throws Exception
	 */
	private byte[] readExcelBytes(FileSource source) throws Exception {
		try (InputStream is = source.getInputStream()) {
			return is.readAllBytes();
		}
	}
}