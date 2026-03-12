package com.neusoft.bsdl.wptool.core.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
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
		byte[] excelBytes;
		try (InputStream is = source.getInputStream()) {
			excelBytes = is.readAllBytes();
		}

		// 操作エリアのバリデーションチェックを実施する
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			validateHeaders(sheet, errors);
			if (!errors.isEmpty()) {
				return null;
			}
		}

		// 操作エリアごとに解析する
		List<DBConfigItemDefinition> processList = new ArrayList<DBConfigItemDefinition>();
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			int lastRowNum = sheet.getLastRowNum();
			int currentRow = DB_CONFIG_SHEET.START_POS_HEADER_INDEX;

			while (currentRow <= lastRowNum) {
				Row row = sheet.getRow(currentRow);
				String cell0 = getCellValue(row, 0).trim();
				if (DBConfigItemDefinitionEnum.FUNCTION_NAME.getDisplayName().equals(cell0)) {
					DBConfigItemDefinition item = new DBConfigItemDefinition();
					item.setDataModel(getCellValue(row, 6).trim());
					item.setOperation(getCellValue(row, 37).trim());
					Row nextRow = sheet.getRow(currentRow + 1);
					if (nextRow != null) {
						item.setOperationCode(getCellValue(nextRow, 6).trim());
						item.setTableName(getCellValue(nextRow, 37).trim());
					}
					// 明細の開始行
					int detailStartRow = currentRow + 4;
					List<DBConfigSubItemDefinition> details = readDetailsFromRow(excelBytes, sheetName, detailStartRow);
					item.setDetails(details);
					processList.add(item);

					currentRow = findNextBlockStart(sheet, detailStartRow + 1);
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
	 * 明細データの読込み（使用 EasyExcel）
	 */
	private List<DBConfigSubItemDefinition> readDetailsFromRow(byte[] excelBytes, String sheetName, int startRow) {
		List<DBConfigSubItemDefinition> details = new ArrayList<>();
		try (InputStream bis = new ByteArrayInputStream(excelBytes)) {
			EasyExcel
					.read(bis, DBConfigSubItemDefinition.class, new AnalysisEventListener<DBConfigSubItemDefinition>() {
						@Override
						public void invoke(DBConfigSubItemDefinition data, AnalysisContext context) {
							if (data != null && !StringUtils.isEmpty(data.getLogicalName())) {
								details.add(data);
							}
						}

						@Override
						public void doAfterAllAnalysed(AnalysisContext context) {
						}
					}).sheet(sheetName).headRowNumber(startRow).doRead();
		} catch (Exception e) {
			log.warn("明细解析失败，startRow={}", startRow, e);
		}
		return details;
	}

	/**
	 * 「DB設定項目定義」シートのすべての操作ブロックに対してヘッダー構造をバリデーション
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
	 * ヘッダ情報のバリデーション
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
	 * 明細のバリデーション
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
	 * 查找下一个操作区块起始行（第一列为 FUNCTION_NAME）
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
}