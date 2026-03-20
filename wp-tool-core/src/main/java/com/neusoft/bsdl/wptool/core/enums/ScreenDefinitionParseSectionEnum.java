package com.neusoft.bsdl.wptool.core.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ScreenDefinitionParseSectionEnum {
    TARGET_MODEL("対象データモデル"),
    PROCESS_MODEL("処理対象データモデル"),
    CONDITION("対象条件"),
    IO_TYPE("入出力タイプ"),
    EXTERNAL_FILES("外部ファイル"),
    INOUT_PART("部分入出力");

    private final String sectionName;

    ScreenDefinitionParseSectionEnum(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getSectionName() {
        return this.sectionName;
    }

    private static final Map<String, ScreenDefinitionParseSectionEnum> BY_SECTION_NAME =
        Arrays.stream(values())
              .collect(Collectors.toMap(
                  ScreenDefinitionParseSectionEnum::getSectionName,
                  Function.identity()
              ));

    public static ScreenDefinitionParseSectionEnum fromSectionName(String sectionName) {
        if (sectionName == null) {
            return null;
        }
        return BY_SECTION_NAME.get(sectionName.trim());
    }

    public static List<String> getAllDisplayNames() {
        return Arrays.stream(values())
                     .map(ScreenDefinitionParseSectionEnum::getSectionName)
                     .collect(Collectors.toList());
    }
}