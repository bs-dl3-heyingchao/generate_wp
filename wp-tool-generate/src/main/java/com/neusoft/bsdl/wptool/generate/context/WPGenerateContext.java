package com.neusoft.bsdl.wptool.generate.context;

import com.neusoft.bsdl.wptool.core.context.WPContext;

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
        this.sqlConverter = new NormalSqlConverter(context.getTableSearchService().listAll());
        // TODO 做成项目用词典
        this.morphemHelper = new MorphemHelper();
    }
}
