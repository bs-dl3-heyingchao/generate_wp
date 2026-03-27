package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import lombok.Data;

/**
 * DB設定項目定義の解析内容
 */
@Data
public class DBConfigDefinition {
    private String sheetName;
    //一覧表示
    private List<DBConfigItemDefinition> processList = Lists.newArrayList();
}
