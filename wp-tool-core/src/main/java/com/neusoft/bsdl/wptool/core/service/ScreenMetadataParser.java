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
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenMetadata;

/**
 * 画面仕様書内の「改版履歴」シートから、画面メタデータ（ヘッダー情報）を抽出するための解析サービス。
 * 
 * <p>このクラスは、Excelの<b>第2行</b>（0起点インデックス = 1）を「メタデータ行」として扱い、
 * 次の情報を {@link ScreenMetadata} オブジェクトにマッピングします：
 * <ul>
 *   <li>システム名</li>
 *   <li>サブシステム名</li>
 *   <li>フェーズ</li>
 *   <li>ドキュメント名</li>
 *   <li>機能分類</li>
 *   <li>機能ID</li>
 *   <li>画面ID</li>
 *   <li>画面名</li>
 * </ul>
 * 
 * <p>解析前に、ヘッダー行（通常は第1行）の列構造が仕様通りかを
 * {@link ModifyHistoryHeaderEnum} を用いて検証します。
 */
public class ScreenMetadataParser extends AbstractParseTool {

	/**
	 * 指定されたExcelファイルの「改版履歴」シートから、画面メタデータを抽出します。
	 * 
	 * <p>処理フロー：
	 * <ol>
	 *   <li>指定シートを取得</li>
	 *   <li>ヘッダー行（{@code MODIFY_HISTORY_SHEET.START_POS_HEADER_INDEX}）の列見出しが仕様と一致するか検証</li>
	 *   <li>検証エラーが存在する場合、{@code null} を返却</li>
	 *   <li>データ行（{@code MODIFY_HISTORY_SHEET.START_POS_DATA_INDEX}）から各メタ項目を抽出</li>
	 * </ol>
	 * 
	 * @param source     解析対象のExcelファイルソース
	 * @param sheetName  対象シート名（通常は「改版履歴」）
	 * @param errors     ヘッダー検証エラーを格納するリスト（null不可）
	 * @return 抽出された画面メタデータ。ヘッダー検証エラー発生時は {@code null}
	 * @throws Exception Excelファイルの読み込みまたはパース中に予期せぬ例外が発生した場合
	 */
	public static ScreenMetadata parseHeaderMetadata(FileSource source, String sheetName, List<ExcelParseError> errors) throws Exception {
		try (InputStream is = source.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
			Sheet sheet = workbook.getSheet(sheetName);
			if (sheet == null) {
				throw new WPParseExcelException("シートが見つかりません: " + sheetName);
			}
			// ヘッダー構造のバリデーション
			validateHeaders(sheet, errors);
			
			if (!CollectionUtils.isEmpty(errors)) {
				return null;
			}

			Row dataRow = sheet.getRow(MODIFY_HISTORY_SHEET.START_POS_DATA_INDEX);

			ScreenMetadata meta = new ScreenMetadata();
			// 各メタ項目を列インデックスに基づき設定
			meta.setSystem(getCellValue(dataRow, ModifyHistoryHeaderEnum.SYSTEM.getColumnIndex()));
			meta.setSubSystem(getCellValue(dataRow, ModifyHistoryHeaderEnum.SUB_SYSTEM.getColumnIndex()));
			meta.setPhase(getCellValue(dataRow, ModifyHistoryHeaderEnum.PHASE.getColumnIndex()));
			meta.setDocumentName(getCellValue(dataRow, ModifyHistoryHeaderEnum.DOCUMENT_NAME.getColumnIndex()));
			meta.setFunctionType(getCellValue(dataRow, ModifyHistoryHeaderEnum.FUNCTION_TYPE.getColumnIndex()));
			meta.setFunctionId(getCellValue(dataRow, ModifyHistoryHeaderEnum.FUNCTION_ID.getColumnIndex()));
			meta.setScreenId(getCellValue(dataRow, ModifyHistoryHeaderEnum.SCREEN_ID.getColumnIndex()));
			meta.setScreenName(getCellValue(dataRow, ModifyHistoryHeaderEnum.SCREEN_NAME.getColumnIndex()));
			return meta;
		}
	}

	/**
	 * 「改版履歴」シートのヘッダー行（通常は第1行）が、仕様定義と一致するかを検証します。
	 * 
	 * <p>{@link ModifyHistoryHeaderEnum} に定義された各列について、
	 * 期待される表示名（{@code displayName}）と実際のセル値が一致するかを確認します。
	 * 不一致が1件でも検出された時点でエラーを追加し、以降の検証は中断します。
	 * 
	 * @param sheet  検証対象のシートオブジェクト
	 * @param errors 検証エラーを格納するリスト（null不可）
	 */
	public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
		Row headerRow = sheet.getRow(MODIFY_HISTORY_SHEET.START_POS_HEADER_INDEX);

		for (ModifyHistoryHeaderEnum header : ModifyHistoryHeaderEnum.values()) {
			String expectedName = header.getDisplayName();
			int expectedIndex = header.getColumnIndex();

			String actualName = getCellValue(headerRow, expectedIndex).trim();

			if (!expectedName.equals(actualName)) {
				errors.add(new ExcelParseError(
						sheet.getSheetName(),
						MODIFY_HISTORY_SHEET.START_POS_HEADER_INDEX + 1, // 表示用行番号は1起点
						expectedIndex + 1,                               // 表示用列番号も1起点（※原文では+1なしだが、一般的には必要）
						MessageService.getMessage("error.format.modifyHistory.wrongColumn")));
				break; // 最初の不一致で即時終了
			}
		}
	}
}