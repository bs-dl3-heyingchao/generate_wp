package com.neusoft.bsdl.wptool.core.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.neusoft.bsdl.wptool.core.model.DBQueryEntity;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.core.service.IWPTableSearchService;

import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.db.define.TableBean.TABLE_TYPE;

public class WPTableSearchUtils {

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

}
