package com.neusoft.bsdl.wptool.generate.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.neusoft.bsdl.wptool.core.model.CsvLayout;
import com.neusoft.bsdl.wptool.core.model.DBConfigDefinition;
import com.neusoft.bsdl.wptool.core.model.DBQueryEntity;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.service.IWPTableSearchService;
import com.neusoft.bsdl.wptool.core.service.impl.WPCombinedTableSearchService;
import com.neusoft.bsdl.wptool.core.service.impl.WPTableSearchService;

import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.db.define.TableBean.TABLE_TYPE;

public class GenerateUtils {

    /**
     * DBQueryシート定義をそのままテーブル検索サービスとして提供する。
     */
    private static class DBQueryTableSearchService extends WPTableSearchService {
        private Map<String, TableBean> tableMap = new LinkedHashMap<>();

        public DBQueryTableSearchService(List<TableBean> tableBeans) {
            for (TableBean tableBean : tableBeans) {
                tableMap.put(tableBean.getTableFullName(), tableBean);
            }
        }

        protected Map<String, TableBean> getTableMap() {
            return tableMap;
        }

        @Override
        public void initialize() {
        }
    }

    public static IWPTableSearchService createCombinedTableSearchService(IWPTableSearchService... tableSearchServices) {
        return new WPCombinedTableSearchService(Arrays.asList(tableSearchServices));
    }

    public static IWPTableSearchService createDBQueryTableSearchService(List<DBQuerySheetContent> dbQuerySheetContents) {
        List<TableBean> dbQueryTableBeans = loadDBQuerySheetContents(dbQuerySheetContents);
        return new DBQueryTableSearchService(dbQueryTableBeans);
    }

    private static List<TableBean> loadDBQuerySheetContents(List<DBQuerySheetContent> dbQuerySheetContents) {
        List<TableBean> dbQueryTableBeans = new ArrayList<>();
        if (dbQuerySheetContents != null && !dbQuerySheetContents.isEmpty()) {
            for (DBQuerySheetContent dbQuerySheetContent : dbQuerySheetContents) {
                String tableFullName = dbQuerySheetContent.getTableName();
                String tableName = dbQuerySheetContent.getTableId();
                TableBean tableBean = new TableBean();
                tableBean.setTableFullName(tableFullName);
                tableBean.setTableName(tableName);
                tableBean.setTableType(TABLE_TYPE.VIEW);
                List<FieldBean> fieldList = new ArrayList<>();
                for (DBQueryEntity item : dbQuerySheetContent.getQueryEntities()) {
                    FieldBean fieldBean = new FieldBean();
                    fieldBean.setFieldName(item.getPhysicalName());
                    fieldBean.setFieldFullName(item.getLogicalName());
                    fieldList.add(fieldBean);
                }
                tableBean.setFieldList(fieldList);
                dbQueryTableBeans.add(tableBean);
            }
        }
        return dbQueryTableBeans;
    }

    public static String addAndGetUniqueCode(String baseCode, Set<String> codeSet) {
        while (!codeSet.add(baseCode)) {
            if (baseCode.matches(".*_\\d+$")) {
                try {
                    String part1 = baseCode.substring(0, baseCode.lastIndexOf("_"));
                    String part2 = baseCode.substring(baseCode.lastIndexOf("_") + 1);
                    int suffixLen = part2.length();
                    int index = Integer.parseInt(part2);
                    suffixLen = Math.max(suffixLen, String.valueOf(index + 1).length());
                    baseCode = part1 + "_" + StringUtils.leftPad(String.valueOf((index + 1)), suffixLen, '0');
                } catch (Exception e) {
                    baseCode = baseCode + "_01";
                }
            } else {
                baseCode = baseCode + "_01";
            }
            baseCode = baseCode.replaceAll("_+", "_");
        }
        return baseCode;
    }

    public static List<ExcelSheetContent<CsvLayout>> filterCsvLayoutSheetContents(List<ExcelSheetContent<?>> sheetContents) {
        List<ExcelSheetContent<CsvLayout>> csvLayoutSheetContents = new ArrayList<>();
        if (sheetContents != null && !sheetContents.isEmpty()) {
            for (ExcelSheetContent<?> sheetContent : sheetContents) {
                if (sheetContent.getContent() instanceof CsvLayout) {
                    @SuppressWarnings("unchecked")
                    ExcelSheetContent<CsvLayout> csvLayoutSheetContent = (ExcelSheetContent<CsvLayout>) sheetContent;
                    csvLayoutSheetContents.add(csvLayoutSheetContent);
                }
            }
        }
        return csvLayoutSheetContents;
    }

    public static List<ExcelSheetContent<DBConfigDefinition>> filterDBConfigSheetContents(List<ScreenExcelContent> excelContents) {
        List<ExcelSheetContent<DBConfigDefinition>> list = new ArrayList<>();
        for (var excelContent : excelContents) {
            var sheetContents = excelContent.getSheetList();
            if (sheetContents != null && !sheetContents.isEmpty()) {
                for (ExcelSheetContent<?> sheetContent : sheetContents) {
                    if (sheetContent.getContent() instanceof DBConfigDefinition) {
                        @SuppressWarnings("unchecked")
                        ExcelSheetContent<DBConfigDefinition> csvLayoutSheetContent = (ExcelSheetContent<DBConfigDefinition>) sheetContent;
                        list.add(csvLayoutSheetContent);
                    }
                }
            }
        }
        return list;
    }
}
