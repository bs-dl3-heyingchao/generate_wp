package com.neusoft.bsdl.wptool.generate.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.neusoft.bsdl.wptool.core.model.CsvLayout;
import com.neusoft.bsdl.wptool.core.model.DBConfigDefinition;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;

import cbai.util.StringUtils;

public class GenerateUtils {

    public static String addAndGetUniqueCode(String baseCode, Set<String> codeSet) {
        while (!codeSet.add(baseCode)) {
            if (baseCode.matches(".*_\\d+$")) {
                try {
                    String part1 = baseCode.substring(0, baseCode.lastIndexOf("_"));
                    String part2 = baseCode.substring(baseCode.lastIndexOf("_") + 1);
                    int suffixLen = part2.length();
                    int index = Integer.parseInt(part2);
                    suffixLen = Math.max(suffixLen, String.valueOf(index + 1).length());
                    baseCode = part1 + "_" + StringUtils.leftPad(String.valueOf((index + 1)), suffixLen, '0');
                } catch (Exception e) {
                    baseCode = baseCode + "_01";
                }
            } else {
                baseCode = baseCode + "_01";
            }
            baseCode = baseCode.replaceAll("_+", "_");
        }
        return baseCode;
    }

    public static List<ExcelSheetContent<CsvLayout>> filterCsvLayoutSheetContents(List<ExcelSheetContent<?>> sheetContents) {
        List<ExcelSheetContent<CsvLayout>> csvLayoutSheetContents = new ArrayList<>();
        if (sheetContents != null && !sheetContents.isEmpty()) {
            for (ExcelSheetContent<?> sheetContent : sheetContents) {
                if (sheetContent.getContent() instanceof CsvLayout) {
                    @SuppressWarnings("unchecked")
                    ExcelSheetContent<CsvLayout> csvLayoutSheetContent = (ExcelSheetContent<CsvLayout>) sheetContent;
                    csvLayoutSheetContents.add(csvLayoutSheetContent);
                }
            }
        }
        return csvLayoutSheetContents;
    }

    public static List<ExcelSheetContent<DBConfigDefinition>> filterDBConfigSheetContents(List<ScreenExcelContent> excelContents) {
        List<ExcelSheetContent<DBConfigDefinition>> list = new ArrayList<>();
        for (var excelContent : excelContents) {
            var sheetContents = excelContent.getSheetList();
            if (sheetContents != null && !sheetContents.isEmpty()) {
                for (ExcelSheetContent<?> sheetContent : sheetContents) {
                    if (sheetContent.getContent() instanceof DBConfigDefinition) {
                        @SuppressWarnings("unchecked")
                        ExcelSheetContent<DBConfigDefinition> csvLayoutSheetContent = (ExcelSheetContent<DBConfigDefinition>) sheetContent;
                        list.add(csvLayoutSheetContent);
                    }
                }
            }
        }
        return list;
    }
}
