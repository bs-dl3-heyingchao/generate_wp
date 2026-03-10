package com.neusoft.bsdl.wptool.check;

import com.neusoft.bsdl.wptool.check.context.WPCheckerContext;
import com.neusoft.bsdl.wptool.core.exception.WPCheckException;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;

import cbai.util.db.define.TableBean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WPScreenChecker {
    private WPCheckerContext context;

    public WPScreenChecker(WPCheckerContext context) {
        this.context = context;
    }

    public void checkScreenData(ScreenExcelContent screenExcelContent) throws WPCheckException {

        // チェックDB項目
        TableBean tableBean = context.getTableSearchService().findTableByFullName("テストDB");
        if (tableBean == null) {
            log.error("テストDBが見つかりませんでした。");
            throw new WPCheckException("テストDBが見つかりませんでした。");
        }
    }
}
