package com.neusoft.bsdl.wptool.generate;

import java.io.File;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WpCodeGenerator {

    private WPContext context;

    public WpCodeGenerator(WPContext context) {
        this.context = context;
    }

    public void generate(ScreenExcelContent screenExcelContent, File outputDir) {
        log.info("Generation flow placeholder created. Implement concrete generation logic here.");
    }
}
