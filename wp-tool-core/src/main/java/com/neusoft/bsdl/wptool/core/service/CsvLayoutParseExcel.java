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
import com.neusoft.bsdl.wptool.core.CommonConstant.CSV_LAYOUT_SHEET;
import com.neusoft.bsdl.wptool.core.enums.CsvLayoutDetailEnum;
import com.neusoft.bsdl.wptool.core.enums.CsvLayoutEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.CsvLayout;
import com.neusoft.bsdl.wptool.core.model.CsvSubLayout;

/**
 * CSVレイアウトのコンテンツの解析ツール
 */
public class CsvLayoutParseExcel extends AbstractParseTool {

	public CsvLayout parseSpecSheet(FileSource source, String sheetName, List<ExcelParseError> errors)
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
		CsvLayout result;
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			result = readHeaderMetadata(sheet);
		}

		// 明細情報を解析する
		List<CsvSubLayout> subLayouts = new ArrayList<>();
		AnalysisEventListener<CsvSubLayout> listener = new AnalysisEventListener<CsvSubLayout>() {
			@Override
			public void invoke(CsvSubLayout row, AnalysisContext context) {
				if (row != null) {
					subLayouts.add(row);
				}
			}

			@Override
			public void doAfterAllAnalysed(AnalysisContext context) {
			}
		};

		try (InputStream bis = new ByteArrayInputStream(excelBytes)) {
			EasyExcel.read(bis, CsvSubLayout.class, listener).sheet(sheetName)
					.headRowNumber(CSV_LAYOUT_SHEET.START_POS_DATA_INDEX).doRead();
		}

		result.setCsvSubLayouts(subLayouts);
		return result;
	}

	/**
	 * 「CSVレイアウト」シートのヘッダー列、明細列構造のバリデーションチェック
	 * 
	 * @param sheet  シートオブジェクト
	 * @param errors エラーオブジェクト
	 */
	public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
		Row level0Header = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX);
		Row level1Header = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX + 1);
		Row level2Header = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX + 2);

		// 主ヘッダ（3行構成）
		for (CsvLayoutEnum header : CsvLayoutEnum.values()) {
			String expected = header.getDisplayName();
			int colIndex = header.getColumnIndex();
			String actual = "";
			Row targetRow = switch (header.getLevel()) {
			case 0 -> level0Header;
			case 1 -> level1Header;
			case 2 -> level2Header;
			default -> null;
			};
			if (targetRow != null) {
				actual = getCellValue(targetRow, colIndex).trim();
			}
			if (!expected.equals(actual)) {
				errors.add(new ExcelParseError(sheet.getSheetName(),
						CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX + header.getLevel() + 1, colIndex + 1,
						MessageService.getMessage("error.format.csvLayout.wrongColumn")));
				break;
			}
		}

		// 明細ヘッダ（2行構成）
		Row detailLevel0 = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_DETAIL_INDEX);
		Row detailLevel1 = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_DETAIL_INDEX + 1);

		for (CsvLayoutDetailEnum header : CsvLayoutDetailEnum.values()) {
			String expected = header.getDisplayName();
			int colIndex = header.getColumnIndex();
			String actual = "";
			Row targetRow = header.getLevel() == 0 ? detailLevel0 : detailLevel1;
			if (targetRow != null) {
				actual = getCellValue(targetRow, colIndex).trim();
			}
			if (!expected.equals(actual)) {
				errors.add(new ExcelParseError(sheet.getSheetName(),
						CSV_LAYOUT_SHEET.START_POS_DETAIL_INDEX + header.getLevel() + 1, colIndex + 1,
						MessageService.getMessage("error.format.csvLayout.wrongColumn")));
				break;
			}
		}
	}

	/**
	 * ヘッダ情報取得（元データ部分）
	 * 
	 * @param sheet
	 * @return
	 */
	private CsvLayout readHeaderMetadata(Sheet sheet) {
		CsvLayout layout = new CsvLayout();

		Row row0 = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX);
		if (row0 != null) {
			layout.setFunctionName(getCellValue(row0, 6));
			layout.setFileId(getCellValue(row0, 20));
			layout.setFileName(getCellValue(row0, 34));
			layout.setInputOutputType(getCellValue(row0, 46));
			layout.setFileFormat(getCellValue(row0, 57));
		}

		Row row1 = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX + 1);
		if (row1 != null) {
			layout.setFileNamingRule(getCellValue(row1, 6));
			layout.setCharacterEncoding(getCellValue(row1, 46));
			layout.setLineEncoding(getCellValue(row1, 157));
		}

		Row row2 = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX + 2);
		if (row2 != null) {
			layout.setSpecialNotes(getCellValue(row2, 6));
		}

		return layout;
	}
}