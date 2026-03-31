package com.neusoft.bsdl.wptool.core.service.impl;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.neusoft.bsdl.wptool.core.WPGlobalUtils;
import com.neusoft.bsdl.wptool.core.cache.ShardedCache;
import com.neusoft.bsdl.wptool.core.exception.WPException;
import com.neusoft.bsdl.wptool.core.reader.WPTableBeanReader;
import com.neusoft.bsdl.wptool.core.service.ConfigService;
import com.neusoft.bsdl.wptool.core.service.IWPTableSearchService;

import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.db.define.reader.ITableBeanReader;
import lombok.extern.slf4j.Slf4j;
import static com.neusoft.bsdl.wptool.core.cache.CacheKeyConst.DB_DEFINE_TABLE_MAP_CACHE_KEY;

@Slf4j
public class WPTableSearchService implements IWPTableSearchService {


    public WPTableSearchService() {
    }

    public synchronized void reIinitialize() {
        File dbExcelDir = ConfigService.getSvnDBDefineDir();
        if (!dbExcelDir.exists()) {
            throw new WPException("DB定義Excelのパスが存在しません: " + dbExcelDir);
        }
        log.info("DB定義Excelからテーブル定義を読み込みます: {}", dbExcelDir.getAbsolutePath());
        ITableBeanReader reader = new WPTableBeanReader(dbExcelDir.getAbsolutePath());
        List<TableBean> list = reader.readTableList();
        Map<String, TableBean> tableMap = new LinkedHashMap<String, TableBean>();
        for (TableBean tableBean : list) {
            tableMap.put(tableBean.getTableFullName(), tableBean);
        }
        WPGlobalUtils.getShardedCache().put(DB_DEFINE_TABLE_MAP_CACHE_KEY, tableMap, 1, TimeUnit.DAYS);
        log.info("DB定義Excelからテーブル定義を読み込みました。テーブル数: {}", tableMap.size());
    }

    public synchronized void initialize() {
        listAll();
    }

    protected Map<String, TableBean> getTableMap() {
        ShardedCache sharedCache = WPGlobalUtils.getShardedCache();
        Map<String, TableBean> cachedTableMap = sharedCache.get(DB_DEFINE_TABLE_MAP_CACHE_KEY);
        if (cachedTableMap == null) {
            synchronized (this) {
                cachedTableMap = sharedCache.get(DB_DEFINE_TABLE_MAP_CACHE_KEY);
                if (cachedTableMap == null) {
                    reIinitialize();
                    cachedTableMap = sharedCache.get(DB_DEFINE_TABLE_MAP_CACHE_KEY);
                    if (cachedTableMap == null) {
                        throw new WPException("テーブル定義の初期化に失敗しました。");
                    }
                }
            }
        }
        return cachedTableMap;
    }

    @Override
    public TableBean findTableByFullName(String tableFullName) {
        assert !getTableMap().isEmpty() : "tableMap is not initialized.";
        return getTableMap().get(tableFullName);
    }

    @Override
    public TableBean findTableByName(String tableName) {
        assert !getTableMap().isEmpty() : "tableMap is not initialized.";
        return getTableMap().values().stream().filter(tableBean -> tableName.equals(tableBean.getTableName())).findFirst().orElse(null);
    }

    @Override
    public FieldBean findFieldByFullName(String tableFullName, String fieldFullName) {
        assert !getTableMap().isEmpty() : "tableMap is not initialized.";
        TableBean tableBean = getTableMap().get(tableFullName);
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
        assert !getTableMap().isEmpty() : "tableMap is not initialized.";
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
        return getTableMap().values().stream().toList();
    }

}
