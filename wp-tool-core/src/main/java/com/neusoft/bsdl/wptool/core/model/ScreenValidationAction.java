package com.neusoft.bsdl.wptool.core.model;

import lombok.Data;

/**
 * チェックアクションのコラム定義
 */
@Data
public class ScreenValidationAction {
    /**
     * アクション名
     */
    private String actionName;

    /**
     * 是否有「〇」标记
     * - true: セルに「〇」
     * - false: 空白、×、△、或其他值
     */
    private boolean hasChecked;
}