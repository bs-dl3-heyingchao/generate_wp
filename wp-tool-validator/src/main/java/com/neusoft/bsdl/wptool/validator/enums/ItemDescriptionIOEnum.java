package com.neusoft.bsdl.wptool.validator.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 画面項目説明書の「IO」列で許可される値の列挙型
 */
public enum ItemDescriptionIOEnum {
    IO("IO入出力"),
    OUTPUT("O出力"),
    ACTION("Aアクション"),
    INPUT("I入力"),
    GROUP("Gグループ");

    private final String displayName;

    ItemDescriptionIOEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static List<String> getAllDisplayNames() {
        return Arrays.stream(values())
                     .map(ItemDescriptionIOEnum::getDisplayName)
                     .collect(Collectors.toList());
    }

    public static boolean isValidDisplayName(String name) {
        return getAllDisplayNames().contains(name);
    }

    public static boolean isValidCode(String code) {
        return Arrays.stream(values())
                     .anyMatch(io -> io.name().equals(code));
    }
}