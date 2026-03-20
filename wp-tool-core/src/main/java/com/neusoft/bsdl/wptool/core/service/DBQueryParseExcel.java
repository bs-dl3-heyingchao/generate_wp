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

	public DBQuerySheetContent parseSpecSheet(FileSource source, String sheetName, List<ExcelParseError> errors)
			throws Exception {
		byte[] excelBytes = readExcelBytes(source);

		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			DBQuerySheetContent content = new DBQuerySheetContent();
			Sheet sheet = workbook.getSheet(sheetName);
			content.setSheetName(sheetName);
			validateDetailHeaders(sheet, errors);
			if (!errors.isEmpty()) {
				return null;
			}

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
			for (int rowIdx = DBQUERY_SHEET.START_POS_DATA_INDEX; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
				Row row = sheet.getRow(rowIdx);
				if (row == null)
					continue;
				String cell0 = getCellValue(row, DBQUERY_SHEET.COL_A).trim();
				if (DBQUERY_SHEET.STR_SECTION_SUMMARY.equals(cell0)) {
					summaryStartRow = rowIdx;
					break;
				}
				DBQueryEntity entity = parseEntityRow(row);
				if (entity != null && !StringUtils.isEmpty(entity.getPhysicalName())) {
					entities.add(entity);
				}
			}
			content.setQueryEntities(entities);

			content.setSummary(parseSummary(sheet, summaryStartRow));

			int queryCondStart = findSectionTitle(sheet, DBQUERY_SHEET.STR_SECTION_QUERY_CONDITION,
					summaryStartRow != -1 ? summaryStartRow + 1 : DBQUERY_SHEET.START_POS_DATA_INDEX);
			content.setQueryCondition(parseSqlBlock(sheet, queryCondStart));

			int aggregateStart = findSectionTitle(sheet, DBQUERY_SHEET.STR_SECTION_QUERY_AGGREGATE_CONDITION,
					queryCondStart != -1 ? queryCondStart + 1
							: (summaryStartRow != -1 ? summaryStartRow + 1 : DBQUERY_SHEET.START_POS_DATA_INDEX));
			content.setQueryAggregateCondition(parseSqlBlock(sheet, aggregateStart));

			int backupStartRow = findSectionTitle(sheet, "備考",
					summaryStartRow != -1 ? summaryStartRow + 1 : DBQUERY_SHEET.START_POS_DATA_INDEX);
			if (backupStartRow != -1) {
				DBQueryJoinCondition joinCond = parseBackupSection(sheet, backupStartRow);
				content.setJoinCondition(joinCond);
			}

			return content;
		}
	}

	private DBQueryJoinCondition parseBackupSection(Sheet sheet, int backupStartRow) {
		DBQueryJoinCondition result = new DBQueryJoinCondition();
		List<DBQueryJoinConditionContents> normalList = new ArrayList<>();
		List<DBQueryJoinConditionUnionAllContents> unionAllList = new ArrayList<>();
		Map<String, String> sectionContents = new HashMap<>();

		// 扫描是否包含 "UNION ALL" 或 "★" 开头的查询
		boolean isUnionAllCase = Boolean.FALSE.booleanValue();

		int scan = backupStartRow + 1;
		while (scan <= sheet.getLastRowNum()) {
			Row r = sheet.getRow(scan);
			if (r != null) {
				String b = getCellValue(r, DBQUERY_SHEET.COL_B).trim();

				if (b.equals(DBQUERY_SHEET.STR_JUDGEMENT_JOIN)) {
					break;
				} else if (b.startsWith("★") && b.endsWith("クエリ")) {
					isUnionAllCase = Boolean.TRUE.booleanValue();
					break;
				}
			}
			scan++;
		}

		// ==================== 情况一：普通模式（无UNION）====================
		if (!isUnionAllCase) {
			// 解析普通 JOIN 条件
			int lastRow = parseNormalJoinBlock(sheet, scan, normalList);
			// 提取 ★ 标题区块内容到 sectionContents
			extractStarSectionContent(sheet, lastRow, sectionContents);
		}

		// ==================== 情况二：UNION 模式 ===================
		else if (isUnionAllCase) {
			unionAllList = parseUnionBlocks(sheet, scan);
		}

		// ==================== 设置结果 ===================
		result.setNormaljoinConditions(normalList);
		result.setUnionAlljoinConditions(unionAllList);
		result.setSectionContents(sectionContents);
		result.setUnionAllCase(isUnionAllCase);
		return result;
	}

	private void extractStarSectionContent(Sheet sheet, int startRow, Map<String, String> sectionContents) {
		int r = startRow;
		while (r <= sheet.getLastRowNum()) {
			Row row = sheet.getRow(r);
			if (row == null) {
				r++;
				continue;
			}

			String queryName = getCellValue(row, DBQUERY_SHEET.COL_B).trim();
			if (queryName.startsWith("★")) {
				StringBuilder content = new StringBuilder();
				int blockEnd = r + 1;

				while (blockEnd <= sheet.getLastRowNum()) {
					Row br = sheet.getRow(blockEnd);
					if (br == null) {
						blockEnd++;
						continue;
					}

					String nextB = getCellValue(br, DBQUERY_SHEET.COL_B).trim();

					if (nextB.startsWith("★") && nextB.endsWith("クエリ")) {
						break;
					}

					String line = extractFullRowText(br);
					if (!line.isEmpty()) {
						if (content.length() > 0)
							content.append("\n");
						content.append(line);
					}
					blockEnd++;
				}

				sectionContents.put(queryName, content.toString());
				r = blockEnd;
			} else {
				r++;
			}
		}
	}

	private int parseNormalJoinBlock(Sheet sheet, int startRow, List<DBQueryJoinConditionContents> list) {
		int r = startRow + 2;
		while (r <= sheet.getLastRowNum()) {
			Row row = sheet.getRow(r);
			String tableName = getCellValue(row, DBQUERY_SHEET.COL_B).trim();
			// --- 遇到新块 ---
			String alias = getCellValue(row, DBQUERY_SHEET.COL_C).trim();
			String method = getCellValue(row, DBQUERY_SHEET.COL_E).trim();
			StringBuilder conditionBuilder = new StringBuilder();

			// 添加首行条件
			String firstCond = extractFullJoinConditionFromFToAD(row);
			if (!firstCond.isEmpty()) {
				conditionBuilder.append(firstCond);
			}

			if (tableName.startsWith("★") && StringUtils.isEmpty(alias) && StringUtils.isEmpty(method)
					&& StringUtils.isEmpty(firstCond)) {
				break;
			}

			// 向下读取连续的条件行（A列为空）
			int next = r + 1;
			while (next <= sheet.getLastRowNum()) {
				Row nextRow = sheet.getRow(next);
				if (nextRow == null) {
					next++;
					continue;
				}

				String nextAText = getCellValue(nextRow, DBQUERY_SHEET.COL_B).trim();
				if (!nextAText.isEmpty()) {
					break; // 遇到下一个表名，停止
				}

				String nextCond = extractFullJoinConditionFromFToAD(nextRow);
				if (!nextCond.isEmpty()) {
					if (conditionBuilder.length() > 0) {
						conditionBuilder.append(DBQUERY_SHEET.STR_CTRL);
					}
					conditionBuilder.append(nextCond);
				}
				next++;
			}

			// 构建结果项
			DBQueryJoinConditionContents item = new DBQueryJoinConditionContents();
			item.setTableName(tableName);
			item.setAlias(alias);
			item.setMethod(method);
			item.setCondition(conditionBuilder.toString());

			list.add(item);
			r = next;
		}
		return r;
	}

	// --- UNION 模式解析 ---
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
			if (queryName.startsWith("★") && queryName.endsWith("クエリ")) {
				// Step 1: 找到块结束
				int blockEnd = r + 1;
				while (blockEnd <= sheet.getLastRowNum()) {
					Row next = sheet.getRow(blockEnd);
					if (next != null) {
						String nextB = getCellValue(next, DBQUERY_SHEET.COL_B).trim();
						if (nextB.startsWith("★") && nextB.endsWith("クエリ")) {
							break;
						}
					}
					blockEnd++;
				}

				// Step 2: 提取 condition 和 sort 文本（去除 ■ 标记）
				StringBuilder whereBuilder = new StringBuilder();
				StringBuilder orderBuilder = new StringBuilder();
				boolean inWhere = false;
				boolean inOrder = false;

				for (int i = r + 1; i < blockEnd; i++) {
					Row metaRow = sheet.getRow(i);
					if (metaRow == null)
						continue;

					String marker = getCellValue(metaRow, DBQUERY_SHEET.COL_B).trim();

					if ("■絞込条件".equals(marker)) {
						inWhere = true;
						inOrder = false;
					} else if ("■ソート条件".equals(marker)) {
						inWhere = false;
						inOrder = true;
					}

					if (inWhere) {
						String line = extractFullRowText(metaRow).trim();
						if (!line.isEmpty() && !line.contains("■")) {
							if (whereBuilder.length() > 0)
								whereBuilder.append("\n");
							whereBuilder.append(line);
						}
					} else if (inOrder) {
						String line = extractFullRowText(metaRow).trim();
						orderBuilder.append(line);
					}
				}

				String conditionText = whereBuilder.toString().replaceAll("^.*■絞込条件.*$", "").trim();
				String sortText = orderBuilder.toString();

				// Step 3: 解析 JOIN 条件（仅当 C列是纯表名）
				List<DBQueryJoinConditionContents> joins = new ArrayList<>();
				for (int i = r + 1; i < blockEnd; i++) {
					Row current = sheet.getRow(i);
					if (current == null)
						continue;

					String tableName = getCellValue(current, DBQUERY_SHEET.COL_B).trim();
					if (tableName.isEmpty() || tableName.startsWith("■")) {
						continue;
					}

					String method = getCellValue(current, DBQUERY_SHEET.COL_E).trim();
					String alias = getCellValue(current, DBQUERY_SHEET.COL_C).trim();

					// 跳过标题行
					if ("テーブル名".equals(tableName) || "結合方式".equals(method)) {
						continue;
					}

					StringBuilder condBuilder = new StringBuilder();
					String firstCond = extractFullJoinConditionFromFToAD(current);
					if (!firstCond.isEmpty()) {
						condBuilder.append(firstCond);
					}

					// 读取延续行（C列为空）
					int nextLine = i + 1;
					while (nextLine < blockEnd) {
						Row nextRow = sheet.getRow(nextLine);
						if (nextRow == null) {
							nextLine++;
							continue;
						}
						String nextTableName = getCellValue(nextRow, DBQUERY_SHEET.COL_B).trim();
						if (!nextTableName.isEmpty()) {
							break;
						}
						String nextCond = extractFullJoinConditionFromFToAD(nextRow);
						if (!nextCond.isEmpty()) {
							if (condBuilder.length() > 0)
								condBuilder.append("\n");
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

					i = nextLine - 1; // 跳过已处理行
				}

				// Step 4: 构建 block
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

	// --- 提取 F~AD 列完整条件 ---
	private String extractFullJoinConditionFromFToAD(Row row) {
		if (row == null)
			return "";
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (int i = DBQUERY_SHEET.COL_F; i <= DBQUERY_SHEET.COL_AD; i++) {
			String val = getCellValue(row, i).trim();
			if (!val.isEmpty()) {
				if (!first)
					sb.append("\t");
				sb.append(val);
				first = false;
			}
		}
		return sb.toString();
	}

	// ==================== 保留你原有的其他方法 ====================

	private DBQuerySummary parseSummary(Sheet sheet, int startRow) {
		// ...（保持你原有逻辑不变）
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
			if (cellA.equals(DBQUERY_SHEET.STR_SECTION_QUERY_CONDITION))
				break;
			if (isSectionHeader(cellB)) {
				String section = extractSectionNumber(cellB);
				if ("1".equals(section)) {
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
					StringBuilder supplementBuilder = new StringBuilder();
					int contentRow = currentRow + 1;
					while (contentRow <= sheet.getLastRowNum()) {
						Row r = sheet.getRow(contentRow);
						if (r == null) {
							contentRow++;
							continue;
						}
						String cellA1 = getCellValue(r, DBQUERY_SHEET.COL_A).trim();
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
					break;
				}
			}
			currentRow++;
		}
		return summary;
	}

	private String extractFullRowText(Row row) {
		if (row == null)
			return "";
		StringBuilder line = new StringBuilder();
		boolean first = true;
		for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
			String value = getCellValue(row, i).trim();
			if (!value.isEmpty()) {
				if (!first)
					line.append("\t");
				line.append(value);
				first = false;
			}
		}
		return line.toString();
	}

	private boolean isSectionHeader(String text) {
		if (text == null || text.isEmpty())
			return false;
		return text.matches(DBQUERY_SHEET.MATCH_FROM_ONE_TO_FIVE);
	}

	private String extractSectionNumber(String header) {
		char c = header.charAt(0);
		if (c >= '1' && c <= '5')
			return String.valueOf(c);
		if (c >= '１' && c <= '５')
			return String.valueOf((char) (c - '１' + '1'));
		return "1";
	}

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
			if (cellA.equals(DBQUERY_SHEET.STR_SECTION_QUERY_AGGREGATE_CONDITION) || cellA.equals("備考")
					|| isSectionHeader(cellA)) {
				break;
			}
			String cellB = getCellValue(row, DBQUERY_SHEET.COL_B).trim();
			if (!cellB.isEmpty()) {
				if (sqlBuilder.length() > 0)
					sqlBuilder.append("\n");
				sqlBuilder.append(cellB);
			}
			currentRow++;
		}
		return sqlBuilder.toString();
	}

	private int findSectionTitle(Sheet sheet, String title, int fromRow) {
		for (int i = fromRow; i <= sheet.getLastRowNum(); i++) {
			Row row = sheet.getRow(i);
			if (row != null && title.equals(getCellValue(row, DBQUERY_SHEET.COL_A).trim())) {
				return i;
			}
		}
		return -1;
	}

	public static void validateDetailHeaders(Sheet sheet, List<ExcelParseError> errors) {
		// ...（保持你原有逻辑不变）
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

	private DBQueryEntity parseEntityRow(Row row) {
		// ...（保持你原有逻辑不变）
		DBQueryEntity entity = new DBQueryEntity();
		entity.setItemNo(getCellValue(row, DBQUERY_SHEET.COL_A));
		entity.setPhysicalName(getCellValue(row, DBQUERY_SHEET.COL_B));
		entity.setLogicalName(getCellValue(row, DBQUERY_SHEET.COL_C));
		entity.setIsNullable(DBQUERY_SHEET.STR_TRUE.equalsIgnoreCase(getCellValue(row, DBQUERY_SHEET.COL_D)));
		entity.setKeyGroup(getCellValue(row, DBQUERY_SHEET.COL_E));
		entity.setLengthPre(getCellValue(row, DBQUERY_SHEET.COL_F));
		entity.setLengthS(getCellValue(row, DBQUERY_SHEET.COL_G));
		entity.setLengthB(getCellValue(row, DBQUERY_SHEET.COL_H));
		entity.setDataTypeWP(getCellValue(row, DBQUERY_SHEET.COL_J));
		entity.setDbDefineType(getCellValue(row, DBQUERY_SHEET.COL_K));
		entity.setDbDefineLength(getCellValue(row, DBQUERY_SHEET.COL_L));
		entity.setDbDefineDecimal(getCellValue(row, DBQUERY_SHEET.COL_M));
		entity.setRemark(getCellValue(row, DBQUERY_SHEET.COL_P));
		entity.setEncodeType(getCellValue(row, DBQUERY_SHEET.COL_AB));
		entity.setResourceTableName(getCellValue(row, DBQUERY_SHEET.COL_AJ));
		entity.setResourceColumnName(getCellValue(row, DBQUERY_SHEET.COL_AK));
		entity.setCaseWhenCondition(getCellValue(row, DBQUERY_SHEET.COL_AL));
		return entity;
	}

	private byte[] readExcelBytes(FileSource source) throws Exception {
		try (InputStream is = source.getInputStream()) {
			return is.readAllBytes();
		}
	}
}