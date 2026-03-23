package com.neusoft.bsdl.wptool.generate;

import java.io.File;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.DBQueryExcelContent;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;

public class WPDBQueryGeneratorTest {

    public static void main(String[] args) throws Exception {
        FileSource source = new LocalFileSource(args[0]);
        File outputDir = new File("target/output/dq", System.currentTimeMillis() + "");
        WPGenerateContext context = new WPGenerateContext(WPContext.create());
        DBQueryExcelContent queryExcelContent = ParseExcelUtils.parseDBQueryExcel(source);
        for (DBQuerySheetContent tb : queryExcelContent.getQuerySheetContents()) {
            new WPDBQueryGenerator(context, tb).generate(outputDir);
        }
    }
}
