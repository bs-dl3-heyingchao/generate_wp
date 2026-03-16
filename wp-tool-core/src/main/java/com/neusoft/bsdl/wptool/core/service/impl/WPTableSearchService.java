package com.neusoft.bsdl.wptool.core.service.impl;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.neusoft.bsdl.wptool.core.exception.WPException;
import com.neusoft.bsdl.wptool.core.reader.WPTableBeanReader;
import com.neusoft.bsdl.wptool.core.service.ConfigService;
import com.neusoft.bsdl.wptool.core.service.IWPTableSearchService;

import cbai.util.FileUtil;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.db.define.reader.ITableBeanReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WPTableSearchService implements IWPTableSearchService {

    private Map<String, TableBean> tableMap = new LinkedHashMap<String, TableBean>();

    public WPTableSearchService() {
    }

    public synchronized void initialize() {
        File cahceFile = ConfigService.getDBDefineCacheFile();
        List<TableBean> list = null;
        if (!cahceFile.exists()) {
            File dbExcelDir = ConfigService.getSvnDBDefineDir();
            if (!dbExcelDir.exists()) {
                throw new WPException("DB定義Excelのパスが存在しません: " + dbExcelDir);
            }
            log.info("DB定義Excelからテーブル定義を読み込みます: {}", dbExcelDir.getAbsolutePath());
            ITableBeanReader reader = new WPTableBeanReader(dbExcelDir.getAbsolutePath());
            list = reader.readTableList();
            FileUtil.writeObjectToFile(list, cahceFile);
            log.info("DB定義Excelからテーブル定義を読み込みました。テーブル数: {}", list.size());
        } else {
            log.info("DB定義のキャッシュファイルからテーブル定義を読み込みます: {}", cahceFile.getAbsolutePath());
            list = (List<TableBean>) FileUtil.readObjectFromFile(cahceFile);
            log.info("DB定義のキャッシュファイルからテーブル定義を読み込みました。テーブル数: {}", list.size());
        }
        for (TableBean tableBean : list) {
            tableMap.put(tableBean.getTableFullName(), tableBean);
        }
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

    @Override
    public List<TableBean> listAll() {
        return tableMap.values().stream().toList();
    }

}
