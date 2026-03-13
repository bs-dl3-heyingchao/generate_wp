package com.neusoft.bsdl.wptool.core.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.util.StringUtils;
import com.neusoft.bsdl.wptool.core.CommonConstant.DBQUERY_SHEET;
import com.neusoft.bsdl.wptool.core.enums.DBQueryHeaderEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.DBQueryEntity;
import com.neusoft.bsdl.wptool.core.model.DBQueryJoinCondition;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.core.model.DBQuerySummary;

import lombok.extern.slf4j.Slf4j;

/**
 * DBクエリ定義書（Excel形式）を解析し、構造化されたモデルに変換するためのツールクラス。
 */
@Slf4j
public class DBQueryParseExcel extends AbstractParseTool {

	/**
	 * 指定されたExcelファイルソースから、指定シート名のDBクエリ定義情報を解析し、
	 * {@link DBQuerySheetContent} オブジェクトとして返却します。
	 * 
	 * <p>解析フロー：
	 * <ol>
	 *   <li>明細ヘッダのバリデーション</li>
	 *   <li>主ヘッダ（テーブル名・ID）の抽出</li>
	 *   <li>明細データ（カラム定義）の読み込み</li>
	 *   <li>「概要」セクションの解析</li>
	 *   <li>「dbQuery：検索条件」「dbQueryAggregate：集計関数の検索条件」のSQLブロック抽出</li>
	 * </ol>
	 * 
	 * @param source  解析対象のExcelファイルソース
	 * @param sheetName 解析対象のシート名
	 * @param errors  解析中に発生したエラーを格納するリスト（null不可）
	 * @return 解析結果の {@link DBQuerySheetContent} オブジェクト。エラー発生時は null
	 * @throws Exception Excelの読み込みまたは解析中に予期せぬ例外が発生した場合
	 */
	public DBQuerySheetContent parseSpecSheet(FileSource source, String sheetName, List<ExcelParseError> errors)
			throws Exception {
		byte[] excelBytes = readExcelBytes(source);

		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);

			// 明細ヘッダ情報のバリデーションチェック
			validateDetailHeaders(sheet, errors);
			if (!errors.isEmpty()) {
				return null;
			}

			// 主ヘッダ情報の解析（第6行）
			Row mainHeaderRow = sheet.getRow(DBQUERY_SHEET.START_POS_HEADER_INDEX);
			if (mainHeaderRow == null) {
				errors.add(new ExcelParseError(sheetName, DBQUERY_SHEET.START_POS_HEADER_INDEX + 1, 1,
						"dbQuery定義書のヘッダ行が見つかりません。"));
				return null;
			}

			DBQuerySheetContent content = new DBQuerySheetContent();
			// テーブル名称
			content.setTableName(getCellValue(mainHeaderRow, DBQUERY_SHEET.COL_C).trim());
			// テーブルID
			content.setTableId(getCellValue(mainHeaderRow, DBQUERY_SHEET.COL_G).trim());

			// 明細データを解析し、概要の開始行を算出する
			int summaryStartRow = -1;
			List<DBQueryEntity> entities = new ArrayList<>();
			for (int rowIdx = DBQUERY_SHEET.START_POS_DATA_INDEX; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
				Row row = sheet.getRow(rowIdx);
				if (row == null)
					continue;
				String cell0 = getCellValue(row, DBQUERY_SHEET.COL_A).trim();
				if (DBQUERY_SHEET.STR_SUMMARY.equals(cell0)) {
					summaryStartRow = rowIdx;
					break;
				}
				DBQueryEntity entity = parseEntityRow(row);
				if (entity != null && !StringUtils.isEmpty(entity.getPhysicalName())) {
					entities.add(entity);
				}
			}
			content.setQueryEntities(entities);

			// 概要コンテンツを解析する
			content.setSummary(parseSummary(sheet, summaryStartRow));

			// dbQuery：検索条件の解析
			int queryCondStart = findSectionTitle(sheet, DBQUERY_SHEET.STR_QUERY_CONDITION,
					summaryStartRow != -1 ? summaryStartRow + 1 : DBQUERY_SHEET.START_POS_DATA_INDEX);
			if (queryCondStart != -1) {
				content.setQueryCondition(parseSqlBlock(sheet, queryCondStart));
			}

			// dbQueryAggregate：集計関数の検索条件の解析
			int aggregateStart = findSectionTitle(sheet, DBQUERY_SHEET.STR_QUERY_AGGREGATE_CONDITION,
					queryCondStart != -1 ? queryCondStart + 1
							: (summaryStartRow != -1 ? summaryStartRow + 1 : DBQUERY_SHEET.START_POS_DATA_INDEX));
			if (aggregateStart != -1) {
				content.setQueryAggregateCondition(parseSqlBlock(sheet, aggregateStart));
			}

			return content;
		}
	}

	/**
	 * 「概要」セクション（1．～5．）を解析し、{@link DBQuerySummary} オブジェクトを構築します。
	 * 
	 * <p>各サブセクション（1～5）はB列にタイトルが記載されており、
	 * その直後の行から内容をB列から読み取ります。
	 * 「3．結合条件」のみ、複数列（B～I列）にわたるテーブル構造を特殊処理で解析します。
	 * 
	 * <p>以下のいずれかの条件で解析を終了します：
	 * <ul>
	 *   <li>A列に「dbQuery：検索条件」が出現</li>
	 *   <li>次のセクションタイトル（例: 「4．...」）が検出</li>
	 * </ul>
	 * 
	 * @param sheet    解析対象のシート
	 * @param startRow 「概要」ラベルが存在する行のインデックス（0起点）
	 * @return 構築された {@link DBQuerySummary} オブジェクト
	 */
	private DBQuerySummary parseSummary(Sheet sheet, int startRow) {
		DBQuerySummary summary = new DBQuerySummary();
		int currentRow = startRow + 1;
		while (currentRow <= sheet.getLastRowNum()) {
			Row row = sheet.getRow(currentRow);
			if (row == null) {
				currentRow++;
				continue;
			}
			// dbQuery：検索条件のセクションから飛び出す
			String cellA = getCellValue(row, DBQUERY_SHEET.COL_A).trim();
			if (cellA.equals(DBQUERY_SHEET.STR_QUERY_CONDITION)) {
				break;
			}
			String cellB = getCellValue(row, DBQUERY_SHEET.COL_B).trim();
			// 新しいタイトルの確認（1. ~ 5.）
			if (isSectionHeader(cellB)) {
				// 番号は大小文字を区別しない
				String section = extractSectionNumber(cellB);
				StringBuilder content = new StringBuilder();
				int contentRow = currentRow + 1;
				while (contentRow <= sheet.getLastRowNum()) {
					Row contentRowObj = sheet.getRow(contentRow);
					if (contentRowObj == null) break;
					String line = getCellValue(contentRowObj, DBQUERY_SHEET.COL_B).trim();
					String cellA1 = getCellValue(contentRowObj, DBQUERY_SHEET.COL_A).trim();
					// 停止条件：新しいセクション、dbQuery：検索条件
					if (isSectionHeader(line) || cellA1.equals(DBQUERY_SHEET.STR_QUERY_CONDITION)) {
						break;
					}
					if (content.length() > 0) {
						content.append(DBQUERY_SHEET.STR_CTRL);
					}
					content.append(line);
					contentRow++;
				}
				switch (section) {
				case "1":
					summary.setTargetTable(content.toString());
					break;
				case "2":
					summary.setTargetTableCondition(content.toString());
					break;
				case "4":
					summary.setSortCondition(content.toString());
					break;
				case "5":
					summary.setSupplement(content.toString());
					break;
				default:
					break;
				}

				// "3．結合条件"の解析（テーブル形式）
				if ("3".equals(section)) {
					DBQueryJoinCondition join = new DBQueryJoinCondition();

					StringBuilder methodBuilder = new StringBuilder();
					StringBuilder tableBuilder = new StringBuilder();
					StringBuilder aliasBuilder = new StringBuilder();
					StringBuilder conditionBuilder = new StringBuilder();

					int joinDataRow = currentRow + 2; // テーブルヘッダの次行から開始

					while (joinDataRow <= sheet.getLastRowNum()) {
						Row joinDataRowObj = sheet.getRow(joinDataRow);
						if (joinDataRowObj == null)
							break;
						//結合方法（B列）
						String method = getCellValue(joinDataRowObj, DBQUERY_SHEET.COL_B).trim();
						//対象テーブル（C列）
						String table = getCellValue(joinDataRowObj, DBQUERY_SHEET.COL_C).trim();
						//テーブル別名（E列）
						String alias = getCellValue(joinDataRowObj, DBQUERY_SHEET.COL_E).trim();
						//結合条件（I列）※現在はI列固定だが、実際はF～K列を横断的に結合すべき
						String condition = getCellValue(joinDataRowObj, DBQUERY_SHEET.COL_I).trim();

						// 空白行 → 終了
						if (method.isEmpty() && table.isEmpty() && alias.isEmpty() && condition.isEmpty()) {
							break;
						}

						// 次のセクションタイトルがB列に出現 → 終了
						if (!method.isEmpty() && isSectionHeader(method)) {
							break;
						}

						// 最初の非空値のみを採用（method/table/alias）
						if (!method.isEmpty() && methodBuilder.length() == 0)
							methodBuilder.append(method);
						if (!table.isEmpty() && tableBuilder.length() == 0)
							tableBuilder.append(table);
						if (!alias.isEmpty() && aliasBuilder.length() == 0)
							aliasBuilder.append(alias);

						// 結合条件は複数行を連結
						if (!condition.isEmpty()) {
							if (conditionBuilder.length() > 0) {
								conditionBuilder.append(" ");
							}
							conditionBuilder.append(condition);
						}

						joinDataRow++;
					}
					join.setMethod(methodBuilder.toString());
					join.setTable(tableBuilder.toString());
					join.setAlias(aliasBuilder.toString());
					join.setCondition(conditionBuilder.toString());
					summary.setJoinCondition(join);
				}

				currentRow = contentRow;
				continue;
			}
			currentRow++;
		}
		return summary;
	}

	/**
	 * 指定された文字列が「1．」～「5．」形式のセクションヘッダかどうかを判定します。
	 * 半角・全角数字および句点（.／．）の組み合わせに対応しています。
	 * 
	 * @param text 判定対象の文字列
	 * @return セクションヘッダにマッチする場合は true、それ以外は false
	 */
	private boolean isSectionHeader(String text) {
		if (text == null || text.isEmpty())
			return false;
		// マッチング：半角数字 1-5 または 全角数字 １-５ + 全角/半角句点
		return text.matches(DBQUERY_SHEET.MATCH_FROM_ONE_TO_FIVE);
	}

	/**
	 * 「1．対象テーブル」などのセクションヘッダ文字列から、先頭の数字（"1"～"5"）を抽出します。
	 * 全角数字は半角に変換して返却します。
	 * 
	 * @param header セクションヘッダ文字列（例: "３．結合条件"）
	 * @return 抽出したセクション番号（"1"～"5"）。不正な入力の場合は "1" を返す
	 */
	private String extractSectionNumber(String header) {
		char c = header.charAt(0);
		if (c >= '1' && c <= '5') {
			return String.valueOf(c);
		} else if (c >= '１' && c <= '５') {
			return String.valueOf((char) (c - '１' + '1'));
		}
		return "1";
	}

	/**
	 * 「dbQuery：検索条件」または「dbQueryAggregate：集計関数の検索条件」などのSQLブロックを解析し、
	 * B列からSQL文を抽出して1つの文字列として返します。
	 * 
	 * <p>解析は指定開始行の次の行から開始され、以下のいずれかの条件で終了します：
	 * <ul>
	 *   <li>空行に到達（かつすでに内容を読み始めている場合）</li>
	 *   <li>次のSQLセクション（例: {@code dbQueryAggregate：...}）がB列に出現</li>
	 * </ul>
	 * 
	 * @param sheet         対象のExcelシート
	 * @param startRowIndex セクションタイトルが存在する行のインデックス（0起点）
	 * @return 抽出されたSQL文。各行の末尾には {@link DBQUERY_SHEET#STR_CTRL} が付加される。内容がない場合は空文字列。
	 */
	private String parseSqlBlock(Sheet sheet, int startRowIndex) {
		StringBuilder sql = new StringBuilder();
		boolean isStarted = false;
		for (int i = startRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
			Row row = sheet.getRow(i);
			if (row == null)
				break;
			String value = getCellValue(row, DBQUERY_SHEET.COL_B).trim();
			// 次のSQLセクションで中断
			if (value.startsWith(DBQUERY_SHEET.STR_QUERY_AGGREGATE_CONDITION)) {
				break;
			}
			if (value.isEmpty()) {
				if (isStarted)
					break; // 内容読み取り中に空行 → 終了
				else
					continue; // まだ始まっていない → スキップ
			}

			isStarted = true;
			sql.append(value).append(DBQUERY_SHEET.STR_CTRL);
		}
		return sql.toString().trim();
	}

	/**
	 * 指定されたシート内で、A列に指定されたタイトル文字列が完全一致する行のインデックスを検索します。
	 * 検索は {@code fromRow} で指定された行から開始され、一致する最初の行番号（0起点）を返します。
	 * 見つからない場合は {@code -1} を返します。
	 * 
	 * @param sheet   対象のExcelシート
	 * @param title   検索するセクションタイトル（例: "dbQuery：検索条件"）
	 * @param fromRow 検索を開始する行インデックス（0起点）
	 * @return タイトルが見つかった行のインデックス。見つからない場合は -1
	 */
	private int findSectionTitle(Sheet sheet, String title, int fromRow) {
		for (int i = fromRow; i <= sheet.getLastRowNum(); i++) {
			Row row = sheet.getRow(i);
			if (row != null && title.equals(getCellValue(row, DBQUERY_SHEET.COL_A).trim())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 明細データのヘッダ行（第9行・第10行）が仕様通りであるかを検証します。
	 * 不一致がある場合、{@code errors} にエラー情報を追加します。
	 * 
	 * @param sheet  検証対象のシート
	 * @param errors 検証エラーを格納するリスト（null不可）
	 */
	public static void validateDetailHeaders(Sheet sheet, List<ExcelParseError> errors) {
		Row level0Row = sheet.getRow(DBQUERY_SHEET.START_POS_DETAIL_INDEX); // 第9行
		Row level1Row = sheet.getRow(DBQUERY_SHEET.START_POS_DETAIL_INDEX + 1); // 第10行

		for (DBQueryHeaderEnum header : DBQueryHeaderEnum.values()) {
			String expected = header.getDisplayName();
			int colIndex = header.getColumnIndex();
			int level = header.getLevel();

			Row targetRow = (level == 0) ? level0Row : level1Row;
			String actual = "";
			if (targetRow != null) {
				actual = getCellValue(targetRow, colIndex).trim();
			}

			if (!expected.equals(actual)) {
				int errorRow = (level == 0) ? DBQUERY_SHEET.START_POS_DETAIL_INDEX + 1
						: DBQUERY_SHEET.START_POS_DETAIL_INDEX + 2;
				errors.add(new ExcelParseError(sheet.getSheetName(), errorRow, colIndex + 1,
						MessageService.getMessage("error.format.dbQuery.wrongColumn")));
			}
		}
	}

	/**
	 * 明細データの1行を解析し、{@link DBQueryEntity} オブジェクトにマッピングします。
	 * 各カラムの位置は {@link DBQUERY_SHEET} 定数に基づいています。
	 * 
	 * @param row 解析対象のExcel行オブジェクト
	 * @return 構築された {@link DBQueryEntity} オブジェクト
	 */
	private DBQueryEntity parseEntityRow(Row row) {
		DBQueryEntity entity = new DBQueryEntity();
		//項番
		entity.setItemNo(getCellValue(row, DBQUERY_SHEET.COL_A));
		//カラム名:物理名
		entity.setPhysicalName(getCellValue(row, DBQUERY_SHEET.COL_B));
		//カラム名:論理名
		entity.setLogicalName(getCellValue(row, DBQUERY_SHEET.COL_C));
		//NULL可
		entity.setIsNullable(DBQUERY_SHEET.STR_TRUE.equalsIgnoreCase(getCellValue(row, DBQUERY_SHEET.COL_D)));
		//キーグループ
		entity.setKeyGroup(getCellValue(row, DBQUERY_SHEET.COL_E));
		//長さ:PRE
		entity.setLengthPre(getCellValue(row, DBQUERY_SHEET.COL_F));
		//長さ:S
		entity.setLengthS(getCellValue(row, DBQUERY_SHEET.COL_G));
		//長さ:B
		entity.setLengthB(getCellValue(row, DBQUERY_SHEET.COL_H));
		//データ型(WP)
		entity.setDataTypeWP(getCellValue(row, DBQUERY_SHEET.COL_J));
		//DB定義:型
		entity.setDbDefineType(getCellValue(row, DBQUERY_SHEET.COL_K));
		//DB定義:桁数
		entity.setDbDefineLength(getCellValue(row, DBQUERY_SHEET.COL_L));
		//DB定義:小数桁
		entity.setDbDefineDecimal(getCellValue(row, DBQUERY_SHEET.COL_M));
		//備考
		entity.setRemark(getCellValue(row, DBQUERY_SHEET.COL_P));
		//使用文字種
		entity.setEncodeType(getCellValue(row, DBQUERY_SHEET.COL_AB));
		//取得元:テーブル名
		entity.setResourceTableName(getCellValue(row, DBQUERY_SHEET.COL_AJ));
		//取得元:カラム名
		entity.setResourceColumnName(getCellValue(row, DBQUERY_SHEET.COL_AK));
		
		return entity;
	}
	
	/**
	 * 指定されたファイルソース（{@link FileSource}）からExcelファイルのバイトデータを読み込み、
	 * バイト配列（{@code byte[]}}）として返します。
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