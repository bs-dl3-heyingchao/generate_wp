package com.neusoft.bsdl.wptool.generate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.helpers.MessageFormatter;

import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;

import cbai.util.StringUtils;
import cbai.util.template.CreateTemplateVelocity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class WPAbstractGenerator<T> {
    protected final static CreateTemplateVelocity createTemplate = new CreateTemplateVelocity("com.neusoft.bsdl.wptool.generate.WP_TEMPLATE", true);
    protected WPGenerateContext context;
    protected String logPrefix = "";
    protected String logSubPrefix = "";
    protected List<T> excelContents;

    private List<String> errorLog = new LinkedList<String>();
    private List<String> warnLog = new LinkedList<String>();

    public WPAbstractGenerator(WPGenerateContext context, T excelContent) {
        this.context = context;
        this.excelContents = new ArrayList<>();
        this.excelContents.add(excelContent);
    }

    public WPAbstractGenerator(WPGenerateContext context, List<T> excelContents) {
        this.context = context;
        this.excelContents = excelContents;
    }

    public void generate(File outputDir) throws IOException {
        errorLog.clear();
        warnLog.clear();
        for (T excelContent : excelContents) {
            for (String templateName : getTemplateNames()) {
                createTemplate.create(outputDir, getReplaceMap(excelContent), templateName);
            }
        }
    }

    public abstract String[] getTemplateNames();

    public abstract Map<String, Object> getReplaceMap(T excelContent);

    protected boolean hasValue(String value) {
        return StringUtils.isNotEmpty(value) && !"－".equals(value) && !"ー".equals(value) && !"-".equals(value);
    }

    protected String escapseXml(String xml) {
//        if (xml != null) {
//            xml = xml.replaceAll("\r\n", "\n").replaceAll("\r", "").replaceAll("\n", "\r\n");
//        }
        return StringEscapeUtils.escapeXml11(xml);
    }

    protected void writeErrorLog(String format, Object... arguments) {
        String rendered = String.format("%s %s ", logPrefix, logSubPrefix) + MessageFormatter.arrayFormat(format, arguments).getMessage();
        log.error(rendered);
        errorLog.add(rendered);
    }

    protected void writeWarnLog(String format, Object... arguments) {
        String rendered = String.format("%s %s ", logPrefix, logSubPrefix) + MessageFormatter.arrayFormat(format, arguments).getMessage();
        log.warn(rendered);
        warnLog.add(rendered);
    }

    public List<String> getLogSnapshotError() {
        return Collections.unmodifiableList(errorLog);
    }

    public List<String> getLogSnapshotWarn() {
        return Collections.unmodifiableList(warnLog);
    }
}
