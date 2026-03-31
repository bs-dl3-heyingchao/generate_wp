package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.compress.utils.Lists;

import lombok.Data;

/**
 * DB設定項目定義の解析内容
 */
@Data
public class DBConfigDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sheetName;
    //一覧表示
    private List<DBConfigItemDefinition> processList = Lists.newArrayList();
}
