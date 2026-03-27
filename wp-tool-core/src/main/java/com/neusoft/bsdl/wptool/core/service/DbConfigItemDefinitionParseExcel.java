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
 * 「DB設定項目定義書」Excelシートを解析し、{@link DBConfigDefinition} モデルに変換するためのツールクラス。
 * 
 * <p>この定義書は「複数の操作ブロック（プロセス単位）」から構成されており、
 * 各ブロックは以下の構造を持ちます：
 * <ul>
 *   <li><b>ヘッダ領域</b>：2行（機能名、データモデル、操作コード、テーブル名など）</li>
 *   <li><b>明細ヘッダ</b>：1行（論理名・物理名・設定内容・備考）</li>
 *   <li><b>明細データ</b>：可変行（各設定項目の詳細）</li>
 * </ul>
 * 
 * <p>解析は「機能名」セル（A列）をブロックの開始マーカーとして、複数ブロックを順次処理します。
 */
@Slf4j
public class DbConfigItemDefinitionParseExcel extends AbstractParseTool {

	/**
	 * 指定されたExcelファイルソースから「DB設定項目定義書」シートを解析し、
	 * {@link DBConfigDefinition} オブジェクトを構築して返却します。
	 * 
	 * <p>処理フロー：
	 * <ol>
	 *   <li>Excel全体をバイト配列として読み込み</li>
	 *   <li>各操作ブロックのヘッダおよび明細ヘッダをバリデーション</li>
	 *   <li>「機能名」セルを起点に、各ブロックのヘッダ情報と明細データを抽出</li>
	 *   <li>全ブロックを {@link DBConfigDefinition#processList} に集約</li>
	 * </ol>
	 * 
	 * @param source     解析対象のExcelファイルソース
	 * @param sheetName  解析対象のシート名
	 * @param errors     バリデーションエラーを格納するリスト（null不可）
	 * @return 解析結果の {@link DBConfigDefinition} オブジェクト。エラー発生時は {@code null}
	 * @throws Exception Excelの読み込みまたは解析中に予期せぬ例外が発生した場合
	 */
	public DBConfigDefinition parseSpecSheet(FileSource source, String sheetName, List<ExcelParseError> errors)
			throws Exception {
		byte[] excelBytes = readExcelBytes(source);

		// ヘッダ情報のバリデーションチェック
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
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
				if (row == null) {
					currentRow++;
					continue;
				}
				String cell0 = getCellValue(row, 0).trim();
				if (DBConfigItemDefinitionEnum.FUNCTION_NAME.getDisplayName().equals(cell0)) {
					DBConfigItemDefinition item = new DBConfigItemDefinition();
					// ヘッダ情報の設定（第1行）
					item.setDataModel(getCellValue(row, 6).trim());      // G列
					item.setOperation(getCellValue(row, 37).trim());     // AK列

					// 第2行（操作コード、テーブル名）
					Row nextRow = sheet.getRow(currentRow + 1);
					if (nextRow != null) {
						item.setOperationCode(getCellValue(nextRow, 6).trim());
						item.setTableName(getCellValue(nextRow, 37).trim());
					}

					// 明細データ範囲の特定（ヘッダ下3行目から次のブロック手前まで）
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
		result.setSheetName(sheetName);
		return result;
	}

	/**
	 * 指定された行範囲（開始行～終了行）から明細データを読み取り、
	 * {@link DBConfigSubItemDefinition} のリストとして返却します。
	 * 
	 * <p>途中で次の操作ブロックの開始マーカー（「機能名」セル）が出現した場合は、
	 * そこで読み込みを中断します。
	 * 
	 * @param sheet     対象シート
	 * @param startRow  明細データの開始行インデックス（0起点）
	 * @param endRow    明細データの終了行インデックス（0起点）
	 * @return 抽出された明細項目のリスト
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
				break; // 次のブロック開始 → 中断
			}

			DBConfigSubItemDefinition detail = parseDetailRow(row);
			if (detail != null && !StringUtils.isEmpty(detail.getLogicalName())) {
				details.add(detail);
			}
		}
		return details;
	}

	/**
	 * 明細データの1行を解析し、{@link DBConfigSubItemDefinition} オブジェクトにマッピングします。
	 * 
	 * <p>各カラムの位置は {@link DBConfigItemDefinitionDetailEnum} に基づいて動的に取得し、
	 * 論理名・物理名・設定内容・備考の4項目を設定します。
	 * 
	 * @param row 解析対象のExcel行オブジェクト
	 * @return 構築された明細項目オブジェクト。無効な行の場合は null
	 */
	private DBConfigSubItemDefinition parseDetailRow(Row row) {
		DBConfigSubItemDefinition detail = new DBConfigSubItemDefinition();

		// DBConfigItemDefinitionDetailEnum に基づき、各列をマッピング
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
	 * 指定行から次の「操作ブロック」の開始行（「機能名」セルが存在する行）を検索します。
	 * 
	 * @param sheet    対象シート
	 * @param fromRow  検索開始行インデックス（0起点）
	 * @return 次のブロックの開始行インデックス。見つからない場合は {@code sheet.getLastRowNum() + 1}
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
	 * シート全体のヘッダ構造をバリデーションします。
	 * 
	 * <p>「機能名」セルを起点に各操作ブロックを走査し、
	 * それぞれのメインヘッダ（2行）および明細ヘッダ（1行）が仕様通りかを検証します。
	 * 
	 * @param sheet  検証対象のExcelシート
	 * @param errors 検証エラーを格納するリスト（null不可）
	 */
	public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
		int currentRow = DB_CONFIG_SHEET.START_POS_HEADER_INDEX;
		int lastRowNum = sheet.getLastRowNum();

		while (currentRow <= lastRowNum) {
			Row row = sheet.getRow(currentRow);
			if (row == null) {
				currentRow++;
				continue;
			}
			String cell0 = getCellValue(row, 0).trim();
			if (DBConfigItemDefinitionEnum.FUNCTION_NAME.getDisplayName().equals(cell0)) {
				// メインヘッダ（2行）の検証
				validateMainHeader(sheet, currentRow, errors);
				if (!CollectionUtils.isEmpty(errors)) {
					return; // 初回エラーで即時終了
				}

				// 明細ヘッダ（1行）の検証
				int detailHeaderRowNum = currentRow + 3;
				Row detailHeaderRow = sheet.getRow(detailHeaderRowNum);
				if (detailHeaderRow != null) {
					validateDetailHeader(detailHeaderRow, detailHeaderRowNum, sheet.getSheetName(), errors);
				} else {
					errors.add(new ExcelParseError(sheet.getSheetName(), detailHeaderRowNum + 1, 1,
							MessageService.getMessage("error.format.dbConfig.wrongColumn")));
				}

				// 次のブロックへジャンプ
				currentRow = findNextBlockStart(sheet, currentRow + 4);
			} else {
				currentRow++;
			}
		}
	}

	/**
	 * 単一の操作ブロックにおけるメインヘッダ（2行）を検証します。
	 * 
	 * <p>{@link DBConfigItemDefinitionEnum} に定義された各ヘッダ項目について、
	 * 期待値と実際のセル値が一致するかを確認します。
	 * 
	 * @param sheet               対象シート
	 * @param mainHeaderStartRow  メインヘッダの開始行インデックス（0起点）
	 * @param errors              エラー格納リスト
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
	 * 明細ヘッダ行（1行）を検証します。
	 * 
	 * <p>{@link DBConfigItemDefinitionDetailEnum} に定義された各明細ヘッダ項目について、
	 * 期待値と実際のセル値が一致するかを確認します。
	 * 
	 * @param detailHeaderRow 対象の明細ヘッダ行
	 * @param rowNum          行番号（エラーメッセージ用、1起点）
	 * @param sheetName       シート名（エラーメッセージ用）
	 * @param errors          エラー格納リスト
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
	 * 指定されたファイルソースからExcelファイルをバイト配列として読み込みます。
	 * 
	 * @param source Excelファイルの入力ソース
	 * @return Excelファイルの内容を表すバイト配列
	 * @throws Exception 入力ストリームの読み込み中にIO例外が発生した場合
	 */
	private byte[] readExcelBytes(FileSource source) throws Exception {
		try (InputStream is = source.getInputStream()) {
			return is.readAllBytes();
		}
	}
}