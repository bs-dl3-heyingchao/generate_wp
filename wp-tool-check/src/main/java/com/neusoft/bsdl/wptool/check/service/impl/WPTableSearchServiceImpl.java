package com.neusoft.bsdl.wptool.check.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import com.neusoft.bsdl.wptool.check.service.WPTableSearchService;

import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;

public class WPTableSearchServiceImpl implements WPTableSearchService {

    private Map<String, TableBean> tableMap = new LinkedHashMap<String, TableBean>();

    public WPTableSearchServiceImpl() {
    }

    public synchronized void initialize() {
        // TODO DBからテーブル定義を読み込んでtableMapに格納する処理を実装する
    }

    @Override
    public TableBean findTableByFullName(String tableFullName) {
        assert !tableMap.isEmpty() : "tableMap is not initialized.";
        return tableMap.get(tableFullName);
    }

    @Override
    public TableBean findTableByName(String tableName) {
        assert !tableMap.isEmpty() : "tableMap is not initialized.";
        return tableMap.values().stream().filter(tableBean -> tableName.equals(tableBean.getTableName())).findFirst().orElse(null);
    }

    @Override
    public FieldBean findFieldByFullName(String tableFullName, String fieldFullName) {
        assert !tableMap.isEmpty() : "tableMap is not initialized.";
        TableBean tableBean = tableMap.get(tableFullName);
        if (tableBean == null) {
            return null;
        }
        if (tableBean.getFieldMap() == null) {
            return null;
        }
        return tableBean.getFieldMap().get(fieldFullName);
    }

    @Override
    public FieldBean findFieldByName(String tableName, String fieldName) {
        assert !tableMap.isEmpty() : "tableMap is not initialized.";
        TableBean tableBean = findTableByName(tableName);
        if (tableBean == null) {
            return null;
        }
        if (tableBean.getFieldList() == null) {
            return null;
        }
        return tableBean.getFieldList().stream().filter(fieldBean -> fieldName.equals(fieldBean.getFieldName())).findFirst().orElse(null);
    }

}
