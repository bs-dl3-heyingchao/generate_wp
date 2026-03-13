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
 * DBクエリ定義書の解析ツール（支持多级表头 + 动态解析概要与SQL条件）
 */
@Slf4j
public class DBQueryParseExcel extends AbstractParseTool {

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
            content.setTableName(getCellValue(mainHeaderRow, DBQUERY_SHEET.COL_C).trim());
            content.setTableId(getCellValue(mainHeaderRow, DBQUERY_SHEET.COL_G).trim());

            // === 第一步：解析明细数据，并记录“概要”起始行 ===
            int summaryStartRow = -1;
            List<DBQueryEntity> entities = new ArrayList<>();
            for (int rowIdx = DBQUERY_SHEET.START_POS_DATA_INDEX; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                String cell0 = getCellValue(row, 0).trim();
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

            // 如果没找到“概要”，仍继续尝试解析其他部分（或报错）
            if (summaryStartRow == -1) {
                log.warn("「概要」セクションが見つかりません。");
                // 可选：errors.add(...) 若必须存在
            } else {
                content.setSummary(parseSummary(sheet, summaryStartRow));
            }

            // === 第二步：查找并解析“dbQuery：検索条件” ===
            int queryCondStart = findSectionTitle(sheet, "dbQuery：検索条件", 
                    summaryStartRow != -1 ? summaryStartRow + 1 : DBQUERY_SHEET.START_POS_DATA_INDEX);
            if (queryCondStart != -1) {
                content.setQueryCondition(parseSqlBlock(sheet, queryCondStart));
            }

            // === 第三步：查找并解析“dbQueryAggregate：...” ===
            int aggregateStart = findSectionTitle(sheet, "dbQueryAggregate：集計関数の検索条件",
                    queryCondStart != -1 ? queryCondStart + 1 : 
                    (summaryStartRow != -1 ? summaryStartRow + 1 : DBQUERY_SHEET.START_POS_DATA_INDEX));
            if (aggregateStart != -1) {
                content.setQueryAggregateCondition(parseSqlBlock(sheet, aggregateStart));
            }

            return content;
        }
    }

    /**
     * 解析「概要」部分（从“概要”标题行开始）
     * 
     * 结构：
     *   B列: "1．対象テーブル"
     *   B列: "MTM5080_組織マスタ"        ← 内容（可能多行）
     *   B列: ""
     *   B列: "2．対象テーブルの条件"
     *   B列: "画面検索条件に従う"
     *   ...
     */
    /**
     * 解析「概要」部分（动态读取 1.~5. 的多行内容）
     */
    private DBQuerySummary parseSummary(Sheet sheet, int startRow) {
        DBQuerySummary summary = new DBQuerySummary();
        int currentRow = startRow + 1; // 跳过“概要”标题行

        while (currentRow <= sheet.getLastRowNum()) {
            Row row = sheet.getRow(currentRow);
            String cellB = getCellValue(row, DBQUERY_SHEET.COL_B).trim();

            // 遇到 dbQuery 大节，立即退出
            if (cellB.startsWith("dbQuery")) {
                break;
            }

            // 检查是否是新的 section 标题（1. ~ 5.）
            if (isSectionHeader(cellB)) {
                // 解析 section 编号
                String section = extractSectionNumber(cellB); // "1", "2", ..., "5"

                // 从下一行开始读取内容块
                StringBuilder content = new StringBuilder();
                int contentRow = currentRow + 1;

                while (contentRow <= sheet.getLastRowNum()) {
                    Row contentRowObj = sheet.getRow(contentRow);
                    String line = getCellValue(contentRowObj, DBQUERY_SHEET.COL_B).trim();

                    // 停止条件：空行 或 新的 section 标题 或 dbQuery
                    if (line.isEmpty() || isSectionHeader(line) || line.startsWith("dbQuery")) {
                        break;
                    }

                    if (content.length() > 0) {
                        content.append("\n");
                    }
                    content.append(line);
                    contentRow++;
                }

                // 设置到 summary
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
                        // 不处理
                        break;
                }

                // 如果是 "3．結合条件"，特殊处理（多行结构化数据）
                if ("3".equals(section)) {
                    List<DBQueryJoinCondition> joins = new ArrayList<>();
                    int joinRow = currentRow + 1;
                    while (joinRow <= sheet.getLastRowNum()) {
                        Row joinRowObj = sheet.getRow(joinRow);
                        String checkLine = getCellValue(joinRowObj, DBQUERY_SHEET.COL_B).trim();

                        // 停止条件
                        if (checkLine.isEmpty() || isSectionHeader(checkLine) || checkLine.startsWith("dbQuery")) {
                            break;
                        }

                        // 解析这一行的结合条件（B~E列）
                        DBQueryJoinCondition join = parseJoinConditionFromContentRow(joinRowObj);
                        if (join != null && !(isEmpty(join.getMethod()) && isEmpty(join.getTable()))) {
                            joins.add(join);
                        }
                        joinRow++;
                    }

                    // 注意：你的 model 是单个对象，但这里可能是多个
                    // 如果必须用单个，取第一个；否则建议改模型为 List
                    if (!joins.isEmpty()) {
                        // 临时方案：只取第一个（或合并？）
                        summary.setJoinCondition(joins.get(0));
                        // TODO: 建议将 DBQuerySummary.joinCondition 改为 List<DBQueryJoinCondition>
                    }
                }

                // 跳过已处理的行
                currentRow = contentRow; // 已经停在停止行（空行/新标题），外层会 +1
                continue;
            }

            currentRow++;
        }

        return summary;
    }
    
    private boolean isSectionHeader(String text) {
        if (text == null || text.isEmpty()) return false;
        // 匹配：半角数字 1-5 或 全角数字 １-５ + 全角/半角句点
        return text.matches("^[1-5１-５][．.].*");
    }
    
    private String extractSectionNumber(String header) {
        char c = header.charAt(0);
        if (c >= '1' && c <= '5') {
            return String.valueOf(c);
        } else if (c >= '１' && c <= '５') {
            return String.valueOf((char)(c - '１' + '1'));
        }
        return "1"; // fallback
    }
    private DBQueryJoinCondition parseJoinConditionFromContentRow(Row row) {
        if (row == null) return null;
        DBQueryJoinCondition join = new DBQueryJoinCondition();
        join.setMethod(getCellValue(row, DBQUERY_SHEET.COL_B)); // B: 結合方法
        join.setTable(getCellValue(row, DBQUERY_SHEET.COL_C));  // C: 対象テーブル
        join.setAlias(getCellValue(row, DBQUERY_SHEET.COL_D));  // D: 別名
        join.setCondition(getCellValue(row, DBQUERY_SHEET.COL_E)); // E: 結合条件
        return join;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 解析 SQL 块（如検索条件、集計条件）
     */
    private String parseSqlBlock(Sheet sheet, int titleRow) {
        StringBuilder sql = new StringBuilder();
        boolean started = false;

        for (int i = titleRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) break;

            String value = getCellValue(row, 0).trim();

            // 遇到下一节标题则停止
            if (value.startsWith("dbQuery")) {
                break;
            }

            if (value.isEmpty()) {
                if (started) break; // 已开始且遇到空行 → 结束
                else continue;      // 尚未开始 → 跳过
            }

            started = true;
            sql.append(value).append("\n");
        }

        return sql.toString().trim();
    }

    /**
     * 从指定行开始查找 section 标题
     */
    private int findSectionTitle(Sheet sheet, String title, int fromRow) {
        for (int i = fromRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && title.equals(getCellValue(row, 0).trim())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 明細のヘッダ情報のバリデーション（两级表头）
     */
    public static void validateDetailHeaders(Sheet sheet, List<ExcelParseError> errors) {
        Row level0Row = sheet.getRow(DBQUERY_SHEET.START_POS_DETAIL_INDEX);       // 第9行
        Row level1Row = sheet.getRow(DBQUERY_SHEET.START_POS_DETAIL_INDEX + 1);   // 第10行

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
                errors.add(new ExcelParseError(
                        sheet.getSheetName(),
                        errorRow,
                        colIndex + 1,
                        MessageService.getMessage("error.format.dbQuery.wrongColumn")
                ));
            }
        }
    }

    /**
     * 解析单行明细数据
     */
    private DBQueryEntity parseEntityRow(Row row) {
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
        return entity;
    }

    /**
     * 读取文件字节
     */
    private byte[] readExcelBytes(FileSource source) throws Exception {
        try (InputStream is = source.getInputStream()) {
            return is.readAllBytes();
        }
    }
}