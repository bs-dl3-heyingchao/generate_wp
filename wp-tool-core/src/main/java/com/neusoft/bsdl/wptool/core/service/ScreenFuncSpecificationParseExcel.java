package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.StringUtils;
import com.neusoft.bsdl.wptool.core.CommonConstant;
import com.neusoft.bsdl.wptool.core.CommonConstant.SCREEN_FUNC_SPECIFICATION_SHEET;
import com.neusoft.bsdl.wptool.core.enums.ScreenFuncSpecificationHeaderEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenFuncSpecification;

/**
 * 画面機能定義書のコンテンツの解析ツール
 */
public class ScreenFuncSpecificationParseExcel extends AbstractParseTool {

	public List<ScreenFuncSpecification> parseSpecSheet(FileSource source, String sheetName,
			List<ExcelParseError> errors) throws Exception {
		// バリデーションチェックを実施する
		try (InputStream validInputStream = source.getInputStream();
				Workbook workbook = WorkbookFactory.create(validInputStream)) {
			Sheet sheet = workbook.getSheet(sheetName);
			// 改版履歴の解析コンテンツのバリエーション
			validateHeaders(sheet, errors);

			if (!CollectionUtils.isEmpty(errors)) {
				return null;
			}
		}
		// 明細情報を解析する
		try (InputStream inputStream = source.getInputStream()) {
			List<ScreenFuncSpecification> result = Lists.newArrayList();

			AnalysisEventListener<ScreenFuncSpecification> listener = new AnalysisEventListener<ScreenFuncSpecification>() {
				@Override
				public void invoke(ScreenFuncSpecification row, AnalysisContext context) {
					// 項番は空白でない場合、該当行が有効とする(項番タイトル行をスキップする)
					if (row != null && !StringUtils.isEmpty(row.getItemNo())) {
						result.add(row);
					}
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
				}
			};

			EasyExcel.read(inputStream, ScreenFuncSpecification.class, listener).sheet(sheetName)
					.headRowNumber(CommonConstant.SCREEN_FUNC_SPECIFICATION_SHEET.START_POS_DATA_INDEX).doRead();

			return result;
		}
	}

	/**
	 * 「画面機能定義書」シートのヘッダー列構造のバリデーションチェック
	 * 
	 * @param sheet  シートオブジェクト
	 * @param errors エラーオブジェクト
	 */
	public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
		Row headerRow = sheet.getRow(SCREEN_FUNC_SPECIFICATION_SHEET.START_POS_HEADER_INDEX);

		for (ScreenFuncSpecificationHeaderEnum header : ScreenFuncSpecificationHeaderEnum.values()) {
			String expectedName = header.getDisplayName();
			int expectedIndex = header.getColumnIndex();

			String actualName = getCellValue(headerRow, expectedIndex).trim();

			if (!expectedName.equals(actualName)) {
				errors.add(new ExcelParseError(sheet.getSheetName(),
						SCREEN_FUNC_SPECIFICATION_SHEET.START_POS_HEADER_INDEX + 1, expectedIndex + 1,
						MessageService.getMessage("error.format.funcSpecification.wrongColumn")));
				break;
			}
		}
	}
}
