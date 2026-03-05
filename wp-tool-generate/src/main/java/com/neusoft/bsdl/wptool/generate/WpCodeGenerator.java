package com.neusoft.bsdl.wptool.generate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(WpCodeGenerator.class);

    public void generate(String inputExcelPath, String outputDir) {
        log.info("Generating code from Excel. inputExcelPath={}, outputDir={}", inputExcelPath, outputDir);
        log.info("Generation flow placeholder created. Implement concrete generation logic here.");
    }
}
