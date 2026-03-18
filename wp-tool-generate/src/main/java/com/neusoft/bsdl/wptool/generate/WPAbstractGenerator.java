package com.neusoft.bsdl.wptool.generate;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;

import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;

import cbai.util.StringUtils;
import cbai.util.template.CreateTemplateVelocity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class WPAbstractGenerator<T> {
    protected final static CreateTemplateVelocity createTemplate = new CreateTemplateVelocity("com.neusoft.bsdl.wptool.generate.WP_TEMPLATE", true);
    protected WPGenerateContext context;
    protected String logPrefix;

    public WPAbstractGenerator(WPGenerateContext context) {
        this.context = context;
    }

    public void generate(T excelContent, File outputDir) throws IOException {
        create(excelContent, outputDir);
    }

    public abstract String[] getTemplateNames();

    public abstract Map<String, Object> getReplaceMap(T excelContent);

    protected boolean hasValue(String value) {
        return StringUtils.isNotEmpty(value) && !"－".equals(value) && !"ー".equals(value) && !"-".equals(value);
    }

    public void create(T excelContent, File outputDir) throws IOException {
        for (String templateName : getTemplateNames()) {
            log.info(logPrefix + "Start creating file for template: {}", templateName);
            createTemplate.create(outputDir, getReplaceMap(excelContent), templateName);
        }
    }

    protected String escapseXml(String xml) {
        return StringEscapeUtils.escapeXml11(xml);
    }

    protected void writeErrorLog(String format, Object... arguments) {
        log.error(logPrefix + format, arguments);
    }

    protected void writeWarnLog(String format, Object... arguments) {
        log.warn(logPrefix + format, arguments);
//        if (logger != null) {
//            logger.warn(logPrefix + format, arguments);
//        }
    }
}
