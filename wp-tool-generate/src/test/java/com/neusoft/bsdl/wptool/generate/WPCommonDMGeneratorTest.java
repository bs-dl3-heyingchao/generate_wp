package com.neusoft.bsdl.wptool.generate;

import java.io.File;
import java.util.List;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;

import cbai.util.db.define.TableBean;

public class WPCommonDMGeneratorTest {

    public static void main(String[] args) throws Exception {
        File outputDir = new File("target/output/dm", System.currentTimeMillis() + "");
        WPGenerateContext context = new WPGenerateContext(WPContext.create());
        List<TableBean> tableList = context.getTableSearchService().listAll();
        for (TableBean tb : tableList) {
            new WPCommonDMGenerator(context, tb).generate(outputDir);
        }
    }
}
