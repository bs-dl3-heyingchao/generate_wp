package com.neusoft.bsdl.wptool.generate;

import java.io.File;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;

public class WPCommonDMGeneratorTest {

    public static void main(String[] args) throws Exception {
        File outputDir = new File("target/output/dm", System.currentTimeMillis() + "");
        WPGenerateContext context = new WPGenerateContext(WPContext.create());
        WPCommonDMGenerator codeGenerator = new WPCommonDMGenerator(context);
        codeGenerator.generateAll(outputDir);
    }

}
