package com.neusoft.bsdl.wptool.core.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;

import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.db.define.reader.AbstractExcelTableBeanReader;
import cbai.util.excel.ExcelUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WPTableBeanReader extends AbstractExcelTableBeanReader {

    public WPTableBeanReader(String excelPath) {
        super(excelPath);
    }

    @Override
    protected boolean isTableBeanSheet(ExcelUtil excelUtil, Sheet sheet, int sheetIndex) {
        if (!StringUtils.equals("テーブル名称", excelUtil.getCellStringValue(sheet, 0, 5).trim())) {
            return false;
        }
        String tableFullName = excelUtil.getCellStringValue(sheet, 2, 5).trim();
        if (tableFullName == null || "".equals(tableFullName)) {
            return false;
        }
        String tableName = excelUtil.getCellStringValue(sheet, 10, 5).trim();
        if (StringUtils.isEmpty(tableName)) {
            return false;
        }
        return true;
    }

    @Override
    protected List<TableBean> readTableBeans(ExcelUtil excelUtil, Sheet sheet) {
        TableBean tableBean = new TableBean();
        String tableName = excelUtil.getCellStringValue(sheet, 10, 5).trim();
        String tableFullName = excelUtil.getCellStringValue(sheet, 2, 5).trim();
        tableBean.setTableFullName(tableFullName);
        tableBean.setTableName(tableName);
        tableBean.setSourceName(excelUtil.getExcelFilePath());
        List<FieldBean> fieldList = new ArrayList<>();
        for (int row = 9; row <= sheet.getLastRowNum(); row++) {
            String no = excelUtil.getCellStringValue(sheet, 0, row).trim();
            if (no.matches("\\d+")) {

                FieldBean fieldBean = new FieldBean();
                String fieldName = excelUtil.getCellStringValue(sheet, 1, row).trim();
                if (StringUtils.isEmpty(fieldName)) {
                    log.warn(excelUtil.getExcelFilePath() + " no=" + no + " fieldName is empty");
                } else {
                    String fieldFullName = excelUtil.getCellStringValue(sheet, 2, row).trim();
                    String type = excelUtil.getCellStringValue(sheet, 15, row).trim();
                    String len = excelUtil.getCellStringValue(sheet, 16, row).trim();
                    String dotLen = excelUtil.getCellStringValue(sheet, 17, row).trim();
                    String key = excelUtil.getCellStringValue(sheet, 4, row).trim();
                    String comment = excelUtil.getCellStringValue(sheet, 19, row).trim();
                    String nullEnable = excelUtil.getCellStringValue(sheet, 3, row).trim();

                    fieldBean.setFieldFullName(fieldFullName);
                    fieldBean.setFieldName(fieldName);
                    fieldBean.setType(type);
                    fieldBean.setLen(len);
                    fieldBean.setDotLen(dotLen);
                    fieldBean.setComment(comment);
                    if ("1".equals(key)) {
                        fieldBean.setKey(true);
                    }
                    fieldBean.setNotNull(!"TRUE".equalsIgnoreCase(nullEnable));

                    Map<String, String> others = new HashMap<>();
                    // データ型(WP)
                    others.put("WP_TYPE", excelUtil.getCellStringValue(sheet, 9, row).trim());
//                    others.put("WP_LEN", excelUtil.getCellStringValue(sheet, 10, row).trim());
//                    others.put("WP_DOTLEN", excelUtil.getCellStringValue(sheet, 11, row).trim());
                    others.put("INIT_VAL", excelUtil.getCellStringValue(sheet, 18, row).trim());
                    fieldBean.setOthers(others);
                    fieldList.add(fieldBean);
                }
            }
        }
        tableBean.setFieldList(fieldList);
        return Arrays.asList(tableBean);
    }

}
