package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.neusoft.bsdl.wptool.core.CommonConstant.MODIFY_HISTORY_SHEET;
import com.neusoft.bsdl.wptool.core.enums.ModifyHistoryHeaderEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseException;
import com.neusoft.bsdl.wptool.core.exception.WPParseException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenMetadata;

/**
 * 画面仕様書のヘッダーメタデータ（第2行）を解析するサービス
 */
public class ScreenMetadataParser extends AbstractParseTool{

	/**
	 * Excel の第2行（0-based index = 1）からメタデータを読み取る
	 *
	 * @param source    Excelファイルソース
	 * @param sheetName 対象シート名
	 * @return ScreenMetadata オブジェクト
	 * @throws Exception 読み込みエラー
	 */
	public static ScreenMetadata parseHeaderMetadata(FileSource source, String sheetName,List<ExcelParseError> errors) throws Exception {
		try (InputStream is = source.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
			Sheet sheet = workbook.getSheet(sheetName);
			if (sheet == null) {
				throw new WPParseException("シートが見つかりません: " + sheetName);
			}
			// バリデーションチェックを実施する
			validateHeaders(sheet,errors);
			
			if(!CollectionUtils.isEmpty(errors)) {
				return null;
			}

			Row dataRow = sheet.getRow(MODIFY_HISTORY_SHEET.START_POS_DATA_INDEX);

			ScreenMetadata meta = new ScreenMetadata();
			// システム
			meta.setSystem(getCellValue(dataRow, ModifyHistoryHeaderEnum.SYSTEM.getColumnIndex()));
			// サブシステム
			meta.setSubSystem(getCellValue(dataRow, ModifyHistoryHeaderEnum.SUB_SYSTEM.getColumnIndex()));
			// フェーズ
			meta.setPhase(getCellValue(dataRow, ModifyHistoryHeaderEnum.PHASE.getColumnIndex()));
			// ドキュメント名
			meta.setDocumentName(getCellValue(dataRow, ModifyHistoryHeaderEnum.DOCUMENT_NAME.getColumnIndex()));
			// 機能分類
			meta.setFunctionType(getCellValue(dataRow, ModifyHistoryHeaderEnum.FUNCTION_TYPE.getColumnIndex()));
			// 機能ID
			meta.setFunctionId(getCellValue(dataRow, ModifyHistoryHeaderEnum.FUNCTION_ID.getColumnIndex()));
			// 画面ID
			meta.setScreenId(getCellValue(dataRow, ModifyHistoryHeaderEnum.SCREEN_ID.getColumnIndex()));
			// 画面名
			meta.setScreenName(getCellValue(dataRow, ModifyHistoryHeaderEnum.SCREEN_NAME.getColumnIndex()));
			return meta;
		}
	}

	/**
	 * 「改版履歴」シートのヘッダー列構造のバリデーションチェック
	 * @param sheet シートオブジェクト
	 * @param errors エラーオブジェクト
	 */
	public static void validateHeaders(Sheet sheet,List<ExcelParseError> errors) {
		Row headerRow = sheet.getRow(MODIFY_HISTORY_SHEET.START_POS_HEADER_INDEX);

		for (ModifyHistoryHeaderEnum header : ModifyHistoryHeaderEnum.values()) {
			String expectedName = header.getDisplayName();
			int expectedIndex = header.getColumnIndex();

			String actualName = getCellValue(headerRow, expectedIndex).trim();

			if (!expectedName.equals(actualName)) {
				errors.add(new ExcelParseError(sheet.getSheetName(), MODIFY_HISTORY_SHEET.START_POS_HEADER_INDEX + 1,
						expectedIndex, MessageService.getMessage("error.format.modifyHistory.wrongColumn")));
				break;
			}
		}
	}
}