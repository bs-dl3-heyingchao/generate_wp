package com.neusoft.bsdl.wptool.generate.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.neusoft.bsdl.wptool.core.context.WPContext;

import cbai.util.db.define.TableBean;
import cbai.util.morphem.MorphemHelper;
import cbai.util.sqlconvert.SqlConverterAbstract;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        super(context.getTableSearchService(), context.getMessageLoaderService(), context.getSessionItemLoaderService());
        List<TableBean> tableList = context.getTableSearchService().listAll();
        List<String[]> extendDic = new ArrayList<>();
        tableList.forEach(table -> {
            extendDic.add(new String[] { table.getTableFullName(), table.getTableName() });
            table.getFieldList().forEach(column -> {
                extendDic.add(new String[] { column.getFieldFullName(), column.getFieldName() });
            });
        });
        // 追加扩展词典
        List<String[]> extDicts = MorphemHelper.readExtendDic("classpath:com/neusoft/bsdl/wptool/generate/EXT_DICT.txt");
        extendDic.addAll(extDicts);
        this.morphemHelper = new MorphemHelper(extendDic);

        this.sqlConverter = new SqlConverterAbstract(tableList) {

            @Override
            protected String customTableAliase(String tableAliase, String tableFullName) {
                if (!tableAliase.matches("\\w+")) {
                    String newTableAliase = morphemHelper.getRomaFromKanji(tableAliase);
                    return "T_" + newTableAliase;
                }
                return super.customTableAliase(tableAliase, tableFullName);
            }

            @Override
            public Map<String, String> prepareLines(List<String> arg0) {
                return null;
            }
        };

    }
}
