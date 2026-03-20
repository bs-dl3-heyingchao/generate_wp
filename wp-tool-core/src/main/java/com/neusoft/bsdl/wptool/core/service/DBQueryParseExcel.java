package com.neusoft.bsdl.wptool.core.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.neusoft.bsdl.wptool.core.model.DBQueryJoinConditionContents;
import com.neusoft.bsdl.wptool.core.model.DBQueryJoinConditionUnionAllContents;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.core.model.DBQuerySummary;

import lombok.extern.slf4j.Slf4j;

/**
 * DBクエリ定義書（Excel形式）を解析し、構造化されたモデルに変換するためのツールクラス。
 */
@Slf4j
public class DBQueryParseExcel extends AbstractParseTool {

	/**
	 * 指定されたシート名のDBクエリ定義書をパースし、{@link DBQuerySheetContent} オブジェクトとして返却します。
	 *
	 * @param source    Excelファイルソース
	 * @param sheetName 対象シート名
	 * @param errors    パース中に発生したエラーを格納するリスト
	 * @return パース結果（エラー発生時はnull）
	 * @throws Exception 入出力例外など
	 */
	public DBQuerySheetContent parseSpecSheet(FileSource source, String sheetName, List<ExcelParseError> errors)
			throws Exception {
		byte[] excelBytes = readExcelBytes(source);

		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			DBQuerySheetContent content = new DBQuerySheetContent();
			Sheet sheet = workbook.getSheet(sheetName);
			content.setSheetName(sheetName);
			// ヘッダ行のバリデーション（カラム名チェック）
			validateDetailHeaders(sheet, errors);
			if (!errors.isEmpty()) {
				return null;
			}

			// メインヘッダ行（テーブル名・IDが記載されている行）
			Row mainHeaderRow = sheet.getRow(DBQUERY_SHEET.START_POS_HEADER_INDEX);
			if (mainHeaderRow == null) {
				errors.add(new ExcelParseError(sheetName, DBQUERY_SHEET.START_POS_HEADER_INDEX + 1, 1,
						"dbQuery定義書のヘッダ行が見つかりません。"));
				return null;
			}

			content.setTableName(getCellValue(mainHeaderRow, DBQUERY_SHEET.COL_C).trim());
			content.setTableId(getCellValue(mainHeaderRow, DBQUERY_SHEET.COL_G).trim());

			int summaryStartRow = -1;
			List<DBQueryEntity> entities = new ArrayList<>();
			// データ行（カラム定義）を最後まで走査し、「【概要】」セクションを探す
			for (int rowIdx = DBQUERY_SHEET.START_POS_DATA_INDEX; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
				Row row = sheet.getRow(rowIdx);
				if (row == null)
					continue;
				String cell0 = getCellValue(row, DBQUERY_SHEET.COL_A).trim();
				// 「【概要】」セルが見つかったら、その位置を記録してループ終了
				if (DBQUERY_SHEET.STR_SECTION_SUMMARY.equals(cell0)) {
					summaryStartRow = rowIdx;
					break;
				}
				// カラム定義行をパース
				DBQueryEntity entity = parseEntityRow(row);
				if (entity != null && !StringUtils.isEmpty(entity.getPhysicalName())) {
					entities.add(entity);
				}
			}
			content.setQueryEntities(entities);

			// 【概要】セクションの内容をパース
			content.setSummary(parseSummary(sheet, summaryStartRow));

			// 【検索条件】セクションの開始位置を検索
			int queryCondStart = findSectionTitle(sheet, DBQUERY_SHEET.STR_SECTION_QUERY_CONDITION,
					summaryStartRow != -1 ? summaryStartRow + 1 : DBQUERY_SHEET.START_POS_DATA_INDEX);
			content.setQueryCondition(parseSqlBlock(sheet, queryCondStart));

			// 【集約条件】セクションの開始位置を検索
			int aggregateStart = findSectionTitle(sheet, DBQUERY_SHEET.STR_SECTION_QUERY_AGGREGATE_CONDITION,
					queryCondStart != -1 ? queryCondStart + 1
							: (summaryStartRow != -1 ? summaryStartRow + 1 : DBQUERY_SHEET.START_POS_DATA_INDEX));
			content.setQueryAggregateCondition(parseSqlBlock(sheet, aggregateStart));

			// 【備考】セクション（結合条件やUNION ALL定義）の開始位置を検索
			int backupStartRow = findSectionTitle(sheet, DBQUERY_SHEET.STR_JUDGEMENT_REMARK,
					summaryStartRow != -1 ? summaryStartRow + 1 : DBQUERY_SHEET.START_POS_DATA_INDEX);
			if (backupStartRow != -1) {
				// 【備考】セクション内の結合条件をパース
				DBQueryJoinCondition joinCond = parseBackupSection(sheet, backupStartRow);
				content.setJoinCondition(joinCond);
			}

			return content;
		}
	}

	/**
	 * 【備考】セクションをパースし、通常のJOIN条件またはUNION ALL条件を抽出します。
	 *
	 * @param sheet          対象シート
	 * @param backupStartRow 【備考】セクションの開始行インデックス
	 * @return 結合条件情報
	 */
	private DBQueryJoinCondition parseBackupSection(Sheet sheet, int backupStartRow) {
		DBQueryJoinCondition result = new DBQueryJoinCondition();
		List<DBQueryJoinConditionContents> normalList = new ArrayList<>();
		List<DBQueryJoinConditionUnionAllContents> unionAllList = new ArrayList<>();
		Map<String, String> sectionContents = new HashMap<>();

		// UNION ALL使用パターンかどうかを判定（「★...クエリ」が存在するか？）
		boolean isUnionAllCase = Boolean.FALSE.booleanValue();

		int scan = backupStartRow + 1;
		while (scan <= sheet.getLastRowNum()) {
			Row r = sheet.getRow(scan);
			if (r != null) {
				String b = getCellValue(r, DBQUERY_SHEET.COL_B).trim();

				// 「結合条件」セクションの終わり（次のセクション開始）を検出
				if (b.equals(DBQUERY_SHEET.STR_JUDGEMENT_JOIN)) {
					break;
				}
				// 「★...クエリ」形式のブロックがあれば、UNION ALLパターンと判断
				else if (b.startsWith(DBQUERY_SHEET.STR_JUDGEMENT_PREFIX)
						&& b.endsWith(DBQUERY_SHEET.STR_JUDGEMENT_QUERY)) {
					isUnionAllCase = Boolean.TRUE.booleanValue();
					break;
				}
			}
			scan++;
		}

		if (!isUnionAllCase) {
			// 通常のJOIN条件ブロックをパース
			int lastRow = parseNormalJoinBlock(sheet, scan, normalList);
			// ★で始まるサブクエリ定義（例：★カード受注明細（集約））を抽出
			extractStarSectionContent(sheet, lastRow, sectionContents);
		} else {
			// UNION ALL 使用パターン：各「★...クエリ」ブロックを個別にパース
			unionAllList = parseUnionBlocks(sheet, scan);
		}

		result.setNormaljoinConditions(normalList);
		result.setUnionAlljoinConditions(unionAllList);
		result.setSectionContents(sectionContents);
		result.setUnionAllCase(isUnionAllCase);
		return result;
	}

	/**
	 * ★で始まるサブクエリ定義（例：★カード受注明細（集約））の内容を抽出し、Mapに格納します。
	 *
	 * @param sheet           対象シート
	 * @param startRow        抽出開始行
	 * @param sectionContents 抽出結果を格納するMap（キー：クエリ名、値：説明テキスト）
	 */
	/**
	 * 指定されたシートから「★クエリ」セクション（例: "★判定_クエリ"）の内容を抽出し、
	 * 各クエリ名をキーとして {@code sectionContents} に格納します。
	 * 
	 * <p>各クエリブロックは、次の「★クエリ」行またはシート終端で終了します。
	 * ブロック内の各行は、A列からAH列までのセル値をタブ区切りで結合して1行として扱います。
	 * 
	 * @param sheet 対象のExcelシート
	 * @param startRow セクション開始行インデックス（0ベース）
	 * @param sectionContents 抽出結果を格納するマップ（キー: クエリ名, 値: 内容文字列）
	 */
	private void extractStarSectionContent(Sheet sheet, int startRow, Map<String, String> sectionContents) {
	    if (sheet == null || sectionContents == null) {
	        return;
	    }

	    int r = startRow;
	    while (r <= sheet.getLastRowNum()) {
	        Row row = sheet.getRow(r);
	        if (row == null) {
	            r++;
	            continue;
	        }

	        String queryName = getCellValue(row, DBQUERY_SHEET.COL_B).trim();
	        if (queryName.startsWith(DBQUERY_SHEET.STR_JUDGEMENT_PREFIX)) {
	            StringBuilder content = new StringBuilder();
	            int blockEnd = r + 1;

	            // 次の★クエリまたはシート終端まで読み込む
	            while (blockEnd <= sheet.getLastRowNum()) {
	                Row br = sheet.getRow(blockEnd);
	                if (br == null) {
	                    blockEnd++;
	                    continue;
	                }

	                String nextB = getCellValue(br, DBQUERY_SHEET.COL_B).trim();
	                // 次の★クエリが来たらブロック終了
	                if (nextB.startsWith(DBQUERY_SHEET.STR_JUDGEMENT_PREFIX)) {
	                    break;
	                }

	                // A列～AH列までの文字列を抽出（後続列は無視）
	                String line = extractRowTextToAH(br);
	                if (!line.isEmpty()) {
	                    if (content.length() > 0) {
	                        content.append(DBQUERY_SHEET.STR_CTRL);
	                    }
	                    content.append(line);
	                }
	                blockEnd++;
	            }

	            sectionContents.put(queryName, content.toString());
	            r = blockEnd; // 次のブロックへジャンプ
	        } else {
	            r++;
	        }
	    }
	}
	
	/**
	 * 指定された行のA列からAH列（両端含む）までの非空セル値を、タブ区切りで結合した文字列を生成します。
	 * 
	 * <p>このメソッドは、Excel行の一部（AH列まで）のみを処理し、後続の列は無視することで
	 * メモリ使用量と処理時間を削減します。
	 * 
	 * @param row 対象のExcel行
	 * @return A列～AH列の非空セル値をタブ区切りで結合した文字列。該当セルがない場合は空文字列。
	 */
	private String extractRowTextToAH(Row row) {
	    if (row == null) {
	        return "";
	    }

	    StringBuilder sb = new StringBuilder();
	    boolean first = true;

	    // 列A（0）から列AH（33）までを対象
	    for (int i = DBQUERY_SHEET.COL_A; i <= DBQUERY_SHEET.COL_AH; i++) {
	        String val = getCellValue(row, i);
	        if (val != null) {
	            val = val.trim();
	            if (!val.isEmpty()) {
	                if (!first) {
	                    sb.append(DBQUERY_SHEET.STR_TAB);
	                }
	                sb.append(val);
	                first = false;
	            }
	        }
	    }

	    return sb.toString();
	}

	/**
	 * 通常のJOIN条件ブロック（テーブル名、別名、結合方式、条件）をパースします。
	 *
	 * @param sheet    対象シート
	 * @param startRow パース開始行（「結合条件」タイトルの次の行）
	 * @param list     結果を格納するリスト
	 * @return 最後に処理した行インデックス（★ブロックの開始位置）
	 */
	private int parseNormalJoinBlock(Sheet sheet, int startRow, List<DBQueryJoinConditionContents> list) {
		int r = startRow + 2; // 実際のデータは2行下から開始（タイトル+空行の後）
		while (r <= sheet.getLastRowNum()) {
			Row row = sheet.getRow(r);
			if (row == null) {
				r++;
				continue;
			}
			String tableName = getCellValue(row, DBQUERY_SHEET.COL_B).trim();
			String alias = getCellValue(row, DBQUERY_SHEET.COL_C).trim();
			String method = getCellValue(row, DBQUERY_SHEET.COL_E).trim();
			StringBuilder conditionBuilder = new StringBuilder();

			// F列～AD列の結合条件を連結（複数セルに分かれている場合に対応）
			String firstCond = extractFullJoinConditionFromFToAD(row);
			if (!firstCond.isEmpty()) {
				conditionBuilder.append(firstCond);
			}

			// ★で始まる行かつ他のカラムが空なら、★ブロックの開始とみなして終了
			if (tableName.startsWith(DBQUERY_SHEET.STR_JUDGEMENT_PREFIX) && StringUtils.isEmpty(alias)
					&& StringUtils.isEmpty(method) && StringUtils.isEmpty(firstCond)) {
				break;
			}

			// 結合条件が複数行にまたがる場合、次のテーブル名が現れるまで継続して読み込む
			int next = r + 1;
			while (next <= sheet.getLastRowNum()) {
				Row nextRow = sheet.getRow(next);
				if (nextRow == null) {
					next++;
					continue;
				}

				String nextAText = getCellValue(nextRow, DBQUERY_SHEET.COL_B).trim();
				if (!nextAText.isEmpty()) {
					break; // 次のテーブル名が来た → 現在のテーブルの条件終了
				}

				String nextCond = extractFullJoinConditionFromFToAD(nextRow);
				if (!nextCond.isEmpty()) {
					if (conditionBuilder.length() > 0) {
						conditionBuilder.append(DBQUERY_SHEET.STR_CTRL); // 改行代わりの区切り文字
					}
					conditionBuilder.append(nextCond);
				}
				next++;
			}

			DBQueryJoinConditionContents item = new DBQueryJoinConditionContents();
			item.setTableName(tableName);
			item.setAlias(alias);
			item.setMethod(method);
			item.setCondition(conditionBuilder.toString());

			list.add(item);
			r = next; // 次のテーブルへ
		}
		return r;
	}

	/**
	 * UNION ALL 使用パターンの各「★...クエリ」ブロックをパースします。
	 *
	 * @param sheet    対象シート
	 * @param startRow パース開始行
	 * @return 各クエリブロックのリスト
	 */
	private List<DBQueryJoinConditionUnionAllContents> parseUnionBlocks(Sheet sheet, int startRow) {
		List<DBQueryJoinConditionUnionAllContents> unionAllList = new ArrayList<>();
		int r = startRow;

		while (r <= sheet.getLastRowNum()) {
			Row row = sheet.getRow(r);
			if (row == null) {
				r++;
				continue;
			}

			String queryName = getCellValue(row, DBQUERY_SHEET.COL_B).trim();
			// 「★...クエリ」形式のブロックを検出
			if (queryName.startsWith(DBQUERY_SHEET.STR_JUDGEMENT_PREFIX)
					&& queryName.endsWith(DBQUERY_SHEET.STR_JUDGEMENT_QUERY)) {

				// ブロックの終了位置を特定（次の★クエリ or シート終端）
				int blockEnd = r + 1;
				while (blockEnd <= sheet.getLastRowNum()) {
					Row next = sheet.getRow(blockEnd);
					if (next != null) {
						String nextB = getCellValue(next, DBQUERY_SHEET.COL_B).trim();
						if (nextB.startsWith(DBQUERY_SHEET.STR_JUDGEMENT_PREFIX)
								&& nextB.endsWith(DBQUERY_SHEET.STR_JUDGEMENT_QUERY)) {
							break;
						}
					}
					blockEnd++;
				}

				// ■絞込条件 と ■ソート順 の内容をそれぞれ抽出
				StringBuilder whereBuilder = new StringBuilder();
				StringBuilder orderBuilder = new StringBuilder();
				boolean inWhere = false;
				boolean inOrder = false;

				for (int i = r + 1; i < blockEnd; i++) {
					Row metaRow = sheet.getRow(i);
					if (metaRow == null)
						continue;

					String marker = getCellValue(metaRow, DBQUERY_SHEET.COL_B).trim();

					if (DBQUERY_SHEET.STR_SECTION_SIBORIKOMI.equals(marker)) {
						inWhere = true;
						inOrder = false;
					} else if (DBQUERY_SHEET.STR_SECTION_SORT.equals(marker)) {
						inWhere = false;
						inOrder = true;
					}

					if (inWhere) {
						String line = extractFullRowText(metaRow).trim();
						// ■マーク自体は含めない
						if (!line.isEmpty() && !line.contains(DBQUERY_SHEET.STR_SIKAKU)) {
							if (whereBuilder.length() > 0)
								whereBuilder.append(DBQUERY_SHEET.STR_CTRL);
							whereBuilder.append(line);
						}
					} else if (inOrder) {
						String line = extractFullRowText(metaRow).trim();
						orderBuilder.append(line);
					}
				}

				String conditionText = whereBuilder.toString().replaceAll("^.*■絞込条件.*$", "").trim();
				String sortText = orderBuilder.toString();

				// このクエリブロック内のJOIN条件（テーブル一覧）をパース
				List<DBQueryJoinConditionContents> joins = new ArrayList<>();
				for (int i = r + 1; i < blockEnd; i++) {
					Row current = sheet.getRow(i);
					if (current == null)
						continue;

					String tableName = getCellValue(current, DBQUERY_SHEET.COL_B).trim();
					if (tableName.isEmpty() || tableName.startsWith(DBQUERY_SHEET.STR_SIKAKU)) {
						continue;
					}

					String method = getCellValue(current, DBQUERY_SHEET.COL_E).trim();
					String alias = getCellValue(current, DBQUERY_SHEET.COL_C).trim();

					// ヘッダ行（「テーブル名」「結合方式」）はスキップ
					if ("テーブル名".equals(tableName) || "結合方式".equals(method)) {
						continue;
					}

					StringBuilder condBuilder = new StringBuilder();
					String firstCond = extractFullJoinConditionFromFToAD(current);
					if (!firstCond.isEmpty()) {
						condBuilder.append(firstCond);
					}

					// 結合条件の継続行を読み込み（B列が空の行）
					int nextLine = i + 1;
					while (nextLine < blockEnd) {
						Row nextRow = sheet.getRow(nextLine);
						if (nextRow == null) {
							nextLine++;
							continue;
						}
						String nextTableName = getCellValue(nextRow, DBQUERY_SHEET.COL_B).trim();
						if (!nextTableName.isEmpty()) {
							break; // 次のテーブル名が来た
						}
						String nextCond = extractFullJoinConditionFromFToAD(nextRow);
						if (!nextCond.isEmpty()) {
							if (condBuilder.length() > 0)
								condBuilder.append(DBQUERY_SHEET.STR_CTRL);
							condBuilder.append(nextCond);
						}
						nextLine++;
					}

					DBQueryJoinConditionContents j = new DBQueryJoinConditionContents();
					j.setMethod(method);
					j.setTableName(tableName);
					j.setAlias(alias);
					j.setCondition(condBuilder.toString());
					joins.add(j);

					i = nextLine - 1; // 処理済みの行をスキップ
				}

				// ブロック情報を構築
				DBQueryJoinConditionUnionAllContents block = new DBQueryJoinConditionUnionAllContents();
				block.setQueryName(queryName);
				block.setJoinConditions(joins);
				block.setCondition(conditionText);
				block.setSort(sortText);
				unionAllList.add(block);

				r = blockEnd;
				continue;
			}
			r++;
		}
		return unionAllList;
	}

	/**
	 * 指定された行（{@link Row}）の列Fから列AD（両端含む）までに記載された非空セルの値を、
	 * タブ区切りで結合して1つの文字列として抽出します。
	 * 
	 * <p>このメソッドは主に「JOIN条件」などの横断的な複数セルにまたがる情報を1行にまとめる用途を想定しています。
	 * 各セル値は前後の空白をトリムした上で評価され、空文字列（または {@code null}）のセルは無視されます。
	 * 
	 * @param row 対象のExcel行。{@code null} の場合は空文字列を返します。
	 * @return 列F～ADの非空セル値をタブ区切りで結合した文字列。該当セルがない場合は空文字列。
	 */
	private String extractFullJoinConditionFromFToAD(Row row) {
		if (row == null)
			return "";
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (int i = DBQUERY_SHEET.COL_F; i <= DBQUERY_SHEET.COL_AD; i++) {
			String val = getCellValue(row, i).trim();
			if (!val.isEmpty()) {
				if (!first)
					sb.append(DBQUERY_SHEET.STR_TAB);
				sb.append(val);
				first = false;
			}
		}
		return sb.toString();
	}

	/**
	 * 指定されたシートから「概要」セクション（例：対象テーブル、補足）を解析し、{@link DBQuerySummary} オブジェクトを構築します。
	 * 
	 * <p>解析は {@code startRow + 1} 行目から開始され、「【検索条件】」セクション（A列）に到達するか、
	 * シート終端に達するまで続きます。
	 * 
	 * <p>現在対応しているサブセクション：
	 * <ul>
	 *   <li>「１．対象テーブル」 → B列の連続非空セルを {@code STR_CTRL} 区切りで結合</li>
	 *   <li>「３．補足」 → 全列の非空行を {@link #extractFullRowText(Row)} で取得し、{@code STR_CTRL} 区切りで結合</li>
	 * </ul>
	 * 
	 * @param sheet 対象のExcelシート（{@code null} であってはならない）
	 * @param startRow 「概要」セクションタイトル行のインデックス（0ベース）。{@code -1} の場合は空の {@code DBQuerySummary} を返す。
	 * @return 解析結果を格納した {@link DBQuerySummary} インスタンス
	 */
	private DBQuerySummary parseSummary(Sheet sheet, int startRow) {
		DBQuerySummary summary = new DBQuerySummary();
		if (startRow == -1)
			return summary;
		int currentRow = startRow + 1;
		while (currentRow <= sheet.getLastRowNum()) {
			Row row = sheet.getRow(currentRow);
			if (row == null) {
				currentRow++;
				continue;
			}
			String cellA = getCellValue(row, DBQUERY_SHEET.COL_A).trim();
			String cellB = getCellValue(row, DBQUERY_SHEET.COL_B).trim();
			// 次のセクション（【検索条件】）に到達したら終了
			if (cellA.equals(DBQUERY_SHEET.STR_SECTION_QUERY_CONDITION))
				break;
			// セクションヘッダ（例：「１．対象テーブル」）を検出
			if (isSectionHeader(cellB)) {
				String section = extractSectionNumber(cellB);
				if ("1".equals(section)) {
					// 「１．対象テーブル」の内容を連結
					StringBuilder tableBuilder = new StringBuilder();
					int nextRow = currentRow + 1;
					while (nextRow <= sheet.getLastRowNum()) {
						Row r = sheet.getRow(nextRow);
						if (r == null)
							break;
						String value = getCellValue(r, DBQUERY_SHEET.COL_B).trim();
						if (value.isEmpty())
							break;
						if (tableBuilder.length() > 0)
							tableBuilder.append(DBQUERY_SHEET.STR_CTRL);
						tableBuilder.append(value);
						nextRow++;
					}
					summary.setTargetTable(tableBuilder.toString().trim());
				} else if ("3".equals(section)) {
					// 「３．補足」の内容をすべて抽出（改行区切り）
					StringBuilder supplementBuilder = new StringBuilder();
					int contentRow = currentRow + 1;
					while (contentRow <= sheet.getLastRowNum()) {
						Row r = sheet.getRow(contentRow);
						if (r == null) {
							contentRow++;
							continue;
						}
						String cellA1 = getCellValue(r, DBQUERY_SHEET.COL_A).trim();
						// 次のセクションに到達したら終了
						if (DBQUERY_SHEET.STR_SECTION_QUERY_CONDITION.equals(cellA1))
							break;
						String lineText = extractFullRowText(r);
						if (!lineText.isEmpty()) {
							if (supplementBuilder.length() > 0)
								supplementBuilder.append(DBQUERY_SHEET.STR_CTRL);
							supplementBuilder.append(lineText);
						}
						contentRow++;
					}
					summary.setSupplement(supplementBuilder.toString());
					break; // 補足は最後の項目なのでbreak
				}
			}
			currentRow++;
		}
		return summary;
	}

	/**
	 * 指定された行（{@link Row}）から、すべての非空セルの値をタブ区切りで結合した1行文字列を生成します。
	 * 
	 * <p>セル値は前後の空白をトリムした上で評価され、空文字列（または {@code null}）のセルは無視されます。
	 * セルのインデックス範囲は {@link Row#getFirstCellNum()} から {@link Row#getLastCellNum()} までです。
	 * 
	 * @param row 対象のExcel行。{@code null} の場合は空文字列を返します。
	 * @return 非空セルの値をタブ区切りで結合した文字列。該当セルがない場合は空文字列。
	 */
	private String extractFullRowText(Row row) {
		if (row == null)
			return "";
		StringBuilder line = new StringBuilder();
		boolean first = true;
		for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
			String value = getCellValue(row, i).trim();
			if (!value.isEmpty()) {
				if (!first)
					line.append(DBQUERY_SHEET.STR_TAB);
				line.append(value);
				first = false;
			}
		}
		return line.toString();
	}

	/**
	 * 指定された文字列がセクションヘッダーであるかどうかを判定します。
	 * 
	 * <p>具体的には、文字列が {@code DBQUERY_SHEET.MATCH_FROM_ONE_TO_FIVE} で定義される正規表現パターンにマッチするか否かで判断します。
	 * このメソッドは、入力文字列が {@code null} や空文字列の場合には必ず {@code false} を返します。
	 * 
	 * @param text 判定対象の文字列
	 * @return 文字列がセクションヘッダーである場合 {@code true}、そうでない場合 {@code false}
	 */
	private boolean isSectionHeader(String text) {
		if (text == null || text.isEmpty())
			return false;
		return text.matches(DBQUERY_SHEET.MATCH_FROM_ONE_TO_FIVE);
	}

	/**
	 * セクション見出し文字列の先頭文字からセクション番号（"1"～"5"）を抽出します。
	 * 
	 * <p>以下の2種類の数字フォーマットに対応しています：
	 * <ul>
	 *   <li>半角数字：{@code '1'} ～ {@code '5'}
	 *   <li>全角数字：{@code '１'} ～ {@code '５'}
	 * </ul>
	 * 
	 * <p>先頭文字が上記のいずれにも該当しない場合、デフォルト値として {@code "1"} を返します。
	 * 
	 * @param header セクション見出し文字列（{@code null} や空文字列であってはならないが、安全のため内部でガードあり）
	 * @return 抽出されたセクション番号（"1"～"5" の半角文字列）。無効な場合は "1"
	 */
	private String extractSectionNumber(String header) {
		char c = header.charAt(0);
		if (c >= '1' && c <= '5')
			return String.valueOf(c);
		if (c >= '１' && c <= '５')
			return String.valueOf((char) (c - '１' + '1'));
		return "1";
	}

	/**
	 * 指定されたシートから、SQLブロック（主にB列に記述されたSQL断片）を抽出して結合します。
	 * 
	 * <p>抽出は {@code startRow + 1} 行目から開始され、以下のいずれかの条件で終了します：
	 * <ul>
	 *   <li>A列に「集計条件」セクション見出しが現れた場合</li>
	 *   <li>A列に「判定備考」セクション見出しが現れた場合</li>
	 *   <li>A列が他のセクション見出しと判定された場合（{@link #isSectionHeader(String)} による）</li>
	 * </ul>
	 * 
	 * <p>B列の非空セル値は、{@link DBQUERY_SHEET#STR_CTRL}（例: 改行やセミコロンなど）で結合されます。
	 * 
	 * @param sheet 対象のExcelシート（{@code null} であってはならない）
	 * @param startRow SQLブロックの直前のセクションタイトル行インデックス（0ベース）。{@code -1} の場合は空文字を返す。
	 * @return 抽出・結合されたSQL文字列。該当する行がない場合は空文字列。
	 */
	private String parseSqlBlock(Sheet sheet, int startRow) {
		StringBuilder sqlBuilder = new StringBuilder();
		if (startRow == -1)
			return "";
		int currentRow = startRow + 1;
		while (currentRow <= sheet.getLastRowNum()) {
			Row row = sheet.getRow(currentRow);
			if (row == null) {
				currentRow++;
				continue;
			}
			String cellA = getCellValue(row, DBQUERY_SHEET.COL_A).trim();
			// 次のセクションに到達したら終了
			if (cellA.equals(DBQUERY_SHEET.STR_SECTION_QUERY_AGGREGATE_CONDITION)
					|| cellA.equals(DBQUERY_SHEET.STR_JUDGEMENT_REMARK) || isSectionHeader(cellA)) {
				break;
			}
			String cellB = getCellValue(row, DBQUERY_SHEET.COL_B).trim();
			if (!cellB.isEmpty()) {
				if (sqlBuilder.length() > 0)
					sqlBuilder.append(DBQUERY_SHEET.STR_CTRL);
				sqlBuilder.append(cellB);
			}
			currentRow++;
		}
		return sqlBuilder.toString();
	}

	/**
	 * 指定されたシート内で、A列（COL_A）に特定のセクションタイトルが記載されている行番号を検索します。
	 * 
	 * <p>検索は {@code fromRow} 行（0ベース）から開始され、シートの最終行まで行われます。
	 * セル値は前後の空白をトリムした上で比較されます。
	 * 
	 * @param sheet 検索対象のExcelシート（{@code null} であってはならない）
	 * @param title 検索するセクションタイトル（例: "【テーブル定義】"）。{@code null} の場合、一致することはありません。
	 * @param fromRow 検索を開始する行インデックス（0ベース）
	 * @return タイトルが見つかった場合、その行インデックス（0ベース）。見つからない場合は {@code -1}
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
	 * Excelシートの明細部ヘッダー（2行構成）が仕様通りであるかを検証します。
	 * 
	 * <p>ヘッダー定義は {@link DBQueryHeaderEnum} で管理されており、各列について
	 * レベル0（上段）またはレベル1（下段）のいずれかに表示されることが期待されます。
	 * 実際のセル値と期待値が一致しない場合、{@code errors} リストにエラーを追加します。
	 * 
	 * @param sheet 検証対象のExcelシート
	 * @param errors 検出されたエラーを格納するリスト（nullであってはならない）
	 */
	public static void validateDetailHeaders(Sheet sheet, List<ExcelParseError> errors) {
		Row level0Row = sheet.getRow(DBQUERY_SHEET.START_POS_DETAIL_INDEX);
		Row level1Row = sheet.getRow(DBQUERY_SHEET.START_POS_DETAIL_INDEX + 1);
		for (DBQueryHeaderEnum header : DBQueryHeaderEnum.values()) {
			String expected = header.getDisplayName();
			int colIndex = header.getColumnIndex();
			int level = header.getLevel();
			Row targetRow = (level == 0) ? level0Row : level1Row;
			String actual = targetRow != null ? getCellValue(targetRow, colIndex).trim() : "";
			if (!expected.equals(actual)) {
				int errorRow = (level == 0) ? DBQUERY_SHEET.START_POS_DETAIL_INDEX + 1
						: DBQUERY_SHEET.START_POS_DETAIL_INDEX + 2;
				errors.add(new ExcelParseError(sheet.getSheetName(), errorRow, colIndex + 1,
						"error.format.dbQuery.wrongColumn"));
			}
		}
	}

	/**
	 * Excelシートの1行（{@link Row}）から {@link DBQueryEntity} オブジェクトを構築します。
	 * 
	 * <p>各セルの値は、事前に定義された列定数（例: {@code DBQUERY_SHEET.COL_A}）に基づいて取得され、
	 * 対応するフィールドにマッピングされます。NULL可カラムは文字列 "TRUE"（大文字小文字を無視）で判定されます。
	 * 
	 * @param row Excelシート内の対象行
	 * @return マッピングされた {@link DBQueryEntity} インスタンス
	 */
	private DBQueryEntity parseEntityRow(Row row) {
		DBQueryEntity entity = new DBQueryEntity();
		// 項番
		entity.setItemNo(getCellValue(row, DBQUERY_SHEET.COL_A));
		// カラム名:物理名
		entity.setPhysicalName(getCellValue(row, DBQUERY_SHEET.COL_B));
		// カラム名:論理名
		entity.setLogicalName(getCellValue(row, DBQUERY_SHEET.COL_C));
		// NULL可
		entity.setIsNullable(DBQUERY_SHEET.STR_TRUE.equalsIgnoreCase(getCellValue(row, DBQUERY_SHEET.COL_D)));
		// キーグループ
		entity.setKeyGroup(getCellValue(row, DBQUERY_SHEET.COL_E));
		// 長さ:PRE
		entity.setLengthPre(getCellValue(row, DBQUERY_SHEET.COL_F));
		// 長さ:S
		entity.setLengthS(getCellValue(row, DBQUERY_SHEET.COL_G));
		// 長さ:B
		entity.setLengthB(getCellValue(row, DBQUERY_SHEET.COL_H));
		// データ型(WP)
		entity.setDataTypeWP(getCellValue(row, DBQUERY_SHEET.COL_J));
		// DB定義:型
		entity.setDbDefineType(getCellValue(row, DBQUERY_SHEET.COL_K));
		// DB定義:桁数
		entity.setDbDefineLength(getCellValue(row, DBQUERY_SHEET.COL_L));
		// DB定義:小数桁
		entity.setDbDefineDecimal(getCellValue(row, DBQUERY_SHEET.COL_M));
		// 備考
		entity.setRemark(getCellValue(row, DBQUERY_SHEET.COL_P));
		// 使用文字種
		entity.setEncodeType(getCellValue(row, DBQUERY_SHEET.COL_AB));
		// リソーステーブル
		entity.setResourceTableName(getCellValue(row, DBQUERY_SHEET.COL_AJ));
		// リソースコラム名称
		entity.setResourceColumnName(getCellValue(row, DBQUERY_SHEET.COL_AK));
		// コンディション
		entity.setCaseWhenCondition(getCellValue(row, DBQUERY_SHEET.COL_AL));
		return entity;
	}

	/**
	 * 指定された {@link FileSource} からExcelファイルの全バイトデータを読み込みます。
	 * 
	 * <p>内部で try-with-resources を使用して、InputStream を確実にクローズします。
	 * ファイルが大きい場合、ヒープメモリ使用量に注意が必要です。
	 * 
	 * @param source Excelファイルを提供する {@link FileSource} オブジェクト
	 * @return Excelファイルのバイト配列
	 * @throws Exception 入出力エラー（例：ファイルが存在しない、アクセス権限がないなど）
	 */
	private byte[] readExcelBytes(FileSource source) throws Exception {
		try (InputStream is = source.getInputStream()) {
			return is.readAllBytes();
		}
	}
}