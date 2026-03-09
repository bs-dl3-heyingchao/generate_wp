package com.neusoft.bsdl.wptool.core.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.StringUtils;
import com.neusoft.bsdl.wptool.core.CommonConstant.SCREEN_VALIDATION_SHEET;
import com.neusoft.bsdl.wptool.core.enums.ScreenValidationHeaderEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenValidation;
import com.neusoft.bsdl.wptool.core.model.ScreenValidationAction;

import lombok.extern.slf4j.Slf4j;

/**
 * 画面チェック仕様書のExcelを解析するサービス
 */
@Slf4j
public class ScreenValidationParseExcel extends AbstractParseTool {

	public List<ScreenValidation> parseSpecSheet(FileSource source, String sheetName, List<ExcelParseError> errors)
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

		// 读取チェックアクション列名（第4行，index=50~78，间隔2列）
		List<String> actionColumnNames = readActionColumnNames(excelBytes, sheetName);

		// 明細情報を解析する
		List<ScreenValidation> result = new ArrayList<>();
		try (InputStream bis = new ByteArrayInputStream(excelBytes)) {
			EasyExcel.read(bis, new AnalysisEventListener<Map<Integer, String>>() {
				@Override
				public void invoke(Map<Integer, String> rowMap, AnalysisContext context) {
					String itemNo = rowMap.get(0);
					if (!isValidItemNo(itemNo)) {
						return;
					}
					ScreenValidation v = buildScreenValidation(rowMap, actionColumnNames);
					result.add(v);
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
				}
			}).sheet(sheetName).headRowNumber(SCREEN_VALIDATION_SHEET.START_POS_DATA_INDEX).doRead();
		}

		return result;
	}

	/**
	 * 「画面チェック仕様書」シートのヘッダー列構造のバリデーションチェック
	 * 
	 * @param sheet  シートオブジェクト
	 * @param errors エラーオブジェクト
	 */
	public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
		Row level0Header = sheet.getRow(SCREEN_VALIDATION_SHEET.START_POS_HEADER_INDEX);
		Row level1Header = sheet.getRow(SCREEN_VALIDATION_SHEET.START_POS_HEADER_INDEX + 1);

		for (ScreenValidationHeaderEnum header : ScreenValidationHeaderEnum.values()) {
			String expected = header.getDisplayName();
			int colIndex = header.getColumnIndex();
			String actual = "";
			Row targetRow = (header.getLevel() == 0) ? level0Header : level1Header;
			if (targetRow != null) {
				actual = getCellValue(targetRow, colIndex).trim();
			}
			if (!expected.equals(actual)) {
				errors.add(new ExcelParseError(sheet.getSheetName(),
						SCREEN_VALIDATION_SHEET.START_POS_HEADER_INDEX + header.getLevel() + 1, colIndex + 1,
						MessageService.getMessage("error.format.validation.wrongColumn")));
				break; // 或 continue，根据需求
			}
		}
	}

	/**
	 * 读取チェックアクション列名（位于 START_POS_INDEX + 1 行，即第4行）
	 */
	private List<String> readActionColumnNames(byte[] excelBytes, String sheetName) throws Exception {
		List<String> names = new ArrayList<>();
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			Row headerRow = sheet.getRow(SCREEN_VALIDATION_SHEET.START_POS_HEADER_INDEX + 1);
			if (headerRow == null) {
				log.warn("チェックアクションヘッダ行（第5行）が存在しません");
				return names;
			}
			int columnIndex = SCREEN_VALIDATION_SHEET.ACTION_START_INDEX;
			for (int i = 0; i < SCREEN_VALIDATION_SHEET.ACTION_COLUMN_COUNT; i++) {
				Cell cell = headerRow.getCell(columnIndex);
				String value = (cell != null) ? cell.toString().trim() : "";
				names.add(value);
				columnIndex += 2;
			}
		}
		return names;
	}

	/**
	 * 从 Map 构建 ScreenValidation 对象
	 */
	private ScreenValidation buildScreenValidation(Map<Integer, String> rowMap, List<String> actionNames) {
		ScreenValidation v = new ScreenValidation();
		v.setItemNo(rowMap.get(0));
		v.setItemName(rowMap.get(1));
		v.setValidationName(rowMap.get(7));
		v.setType(rowMap.get(18));
		v.setValidationRule(rowMap.get(22));
		v.setMassageId(rowMap.get(80));
		v.setMessageContent(rowMap.get(85));
		v.setParameter1(rowMap.get(111));
		v.setParameter2(rowMap.get(117));
		v.setParameter3(rowMap.get(123));
		v.setParameter4(rowMap.get(129));
		v.setParameter5(rowMap.get(135));
		v.setRemarks(rowMap.get(141));
		v.setBizWarining(rowMap.get(177));
		v.setCodingMemo(rowMap.get(178));
		v.setMemo(rowMap.get(210));

		// 构建アクションリスト
		List<ScreenValidationAction> actions = new ArrayList<>();
		int colIndex = SCREEN_VALIDATION_SHEET.ACTION_START_INDEX;
		for (int i = 0; i < actionNames.size(); i++) {
			String actionName = actionNames.get(i);
			if (StringUtils.isEmpty(actionName)) {
				break;
			}
			String cellValue = rowMap.get(colIndex);
			ScreenValidationAction action = new ScreenValidationAction();
			action.setActionName(actionName);
			action.setHasChecked(SCREEN_VALIDATION_SHEET.MARK.equals(cellValue));
			actions.add(action);
			colIndex += 2;
		}
		v.setValidationActions(actions);
		return v;
	}

	/**
	 * 判断项番是否有效（非空且为数字）
	 */
	private boolean isValidItemNo(String itemNo) {
		if (itemNo == null || itemNo.trim().isEmpty()) {
			return false;
		}
		try {
			Integer.parseInt(itemNo.trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}