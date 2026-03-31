package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;

import lombok.Data;

/**
 * チェックアクションのコラム定義
 */
@Data
public class ScreenValidationAction implements Serializable {
    private static final long serialVersionUID = 1L;
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