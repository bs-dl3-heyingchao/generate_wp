package com.neusoft.bsdl.wptool.generate;

import java.io.File;

import com.neusoft.bsdl.wptool.core.context.WPContext;

public class WPCommonDMGeneratorTest {

    public static void main(String[] args) throws Exception {
        File outputDir = new File("target/output/dm", System.currentTimeMillis() + "");
        WPContext context = WPContext.create();
        WPCommonDMGenerator codeGenerator = new WPCommonDMGenerator(context);
        codeGenerator.generate(outputDir);
    }

}
