package com.neusoft.bsdl.wptool.generate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.helpers.MessageFormatter;

import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;

import cbai.util.StringUtils;
import cbai.util.template.CreateTemplateVelocity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class WPAbstractGenerator<T> {

    public static final String[][] ZEN_HEN_TABLE = new String[][] { { "１", "1" }, { "２", "2" }, { "３", "3" }, { "４", "4" }, { "５", "5" }, { "６", "6" }, { "７", "7" }, { "８", "8" }, { "９", "9" },
            { "０", "0" }, { "Ａ", "A" }, { "Ｂ", "B" }, { "Ｃ", "C" }, { "Ｄ", "D" }, { "Ｅ", "E" }, { "Ｆ", "F" }, { "Ｇ", "G" }, { "Ｈ", "H" }, { "Ｉ", "I" }, { "Ｊ", "J" }, { "Ｋ", "K" }, { "Ｌ", "L" },
            { "Ｍ", "M" }, { "Ｎ", "N" }, { "Ｏ", "O" }, { "Ｐ", "P" }, { "Ｑ", "Q" }, { "Ｒ", "R" }, { "Ｓ", "S" }, { "Ｔ", "T" }, { "Ｕ", "U" }, { "Ｖ", "V" }, { "Ｗ", "W" }, { "Ｘ", "X" }, { "Ｙ", "Y" },
            { "Ｚ", "Z" }, { "ａ", "a" }, { "ｂ", "b" }, { "ｃ", "c" }, { "ｄ", "d" }, { "ｅ", "e" }, { "ｆ", "f" }, { "ｇ", "g" }, { "ｈ", "h" }, { "ｉ", "i" }, { "ｊ", "j" }, { "ｋ", "k" }, { "ｌ", "l" },
            { "ｍ", "m" }, { "ｎ", "n" }, { "ｏ", "o" }, { "ｐ", "p" }, { "ｑ", "q" }, { "ｒ", "r" }, { "ｓ", "s" }, { "ｔ", "t" }, { "ｕ", "u" }, { "ｖ", "v" }, { "ｗ", "w" }, { "ｘ", "x" }, { "ｙ", "y" },
            { "ｚ", "z" }, { "＠", "@" }, { "＋", "+" } };

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

    protected String convertZenToHen(String value) {
        return convertZenToHen(value, null);
    }

    protected String convertZenToHen(String value, String fieldName) {
        if (value != null) {
            Set<String> zenChars = new LinkedHashSet<>();
            for (String[] pair : ZEN_HEN_TABLE) {
                if (value.contains(pair[0])) {
                    value = value.replace(pair[0], pair[1]);
                    zenChars.add(pair[0]);
                }
            }
            if (!zenChars.isEmpty()) {
                String values = StringUtils.join(zenChars.toArray(), " ");
                if (fieldName != null) {
                    writeWarnLog("{}:全角の「{}」を半角に置換した。", fieldName, values);
                } else {
                    writeWarnLog("全角の「{}」を半角に置換した。", values);
                }
            }
        }
        return value;
    }

    public void generate(File outputDir) throws IOException {
        errorLog.clear();
        warnLog.clear();
        for (T excelContent : excelContents) {
            for (String templateName : getTemplateNames()) {
                Map<String, Object> replaceMap = getReplaceMap(excelContent);
                if (replaceMap != null) {
                    createTemplate.create(outputDir, replaceMap, templateName);
                }
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
        if(logPrefix.startsWith("[ZZZ")) {
            // 共通部分の生成コードのログは出力しない
            return;
        }
        String rendered = String.format("%s %s ", logPrefix, logSubPrefix) + MessageFormatter.arrayFormat(format, arguments).getMessage();
        log.error(rendered);
        errorLog.add(rendered);
    }

    protected void writeWarnLog(String format, Object... arguments) {
        if(logPrefix.startsWith("[ZZZ")) {
            // 共通部分の生成コードのログは出力しない
            return;
        }
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
