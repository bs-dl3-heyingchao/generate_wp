package com.neusoft.bsdl.wptool.generate;

import java.io.File;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;

public class WpCodeGeneratorTest {

    public static void main(String[] args) throws Exception {
        File outputDir = new File("target/output/io", System.currentTimeMillis() + "");
        FileSource source = new LocalFileSource(args[0]);
        WPGenerateContext context = new WPGenerateContext(WPContext.create());
        WPIOGenerator codeGenerator = new WPIOGenerator(context);
        ScreenExcelContent screenExcelContent = ParseExcelUtils.parseScreenExcel(source);
        codeGenerator.generate(screenExcelContent, outputDir);
    }

}
