package com.neusoft.bsdl.wptool.generate.context;

import java.util.ArrayList;
import java.util.List;

import com.neusoft.bsdl.wptool.core.context.WPContext;

import cbai.util.db.define.TableBean;
import cbai.util.morphem.MorphemHelper;
import cbai.util.sqlconvert.NormalSqlConverter;
import cbai.util.sqlconvert.SqlConverterAbstract;

public class WPGenerateContext extends WPContext {
    private MorphemHelper morphemHelper;

    public MorphemHelper getMorphemHelper() {
        return morphemHelper;
    }

    public SqlConverterAbstract getSqlConverter() {
        return sqlConverter;
    }

    private SqlConverterAbstract sqlConverter;

    public WPGenerateContext(WPContext context) {
        super(context.getTableSearchService(), context.getMessageLoaderService());
        List<TableBean> tableList = context.getTableSearchService().listAll();
        this.sqlConverter = new NormalSqlConverter(tableList);
        List<String[]> extendDic = new ArrayList<>();
        tableList.forEach(table -> {
            extendDic.add(new String[] { table.getTableName(), table.getTableName() });
            table.getFieldList().forEach(column -> {
                extendDic.add(new String[] { column.getFieldFullName(), column.getFieldName() });
            });
        });
        // 追加扩展词典
        List<String[]> extDicts = MorphemHelper.readExtendDic("classpath:com/neusoft/bsdl/wptool/generate/EXT_DICT.txt");
        extendDic.addAll(extDicts);
        this.morphemHelper = new MorphemHelper(extendDic);
    }
}
