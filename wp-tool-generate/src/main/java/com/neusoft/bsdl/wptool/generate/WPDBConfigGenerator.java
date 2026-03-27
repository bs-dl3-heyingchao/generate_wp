package com.neusoft.bsdl.wptool.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.neusoft.bsdl.wptool.core.model.DBConfigDefinition;
import com.neusoft.bsdl.wptool.core.model.DBConfigItemDefinition;
import com.neusoft.bsdl.wptool.core.model.DBConfigSubItemDefinition;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;
import com.neusoft.bsdl.wptool.generate.model.DmItem;
import com.neusoft.bsdl.wptool.generate.model.DmOpe;
import com.neusoft.bsdl.wptool.generate.model.DmOpeLogic;

import cbai.util.StringUtils;
import cbai.util.db.define.TableBean;

public class WPDBConfigGenerator extends WPAbstractGenerator<DBConfigDefinition> {

    public WPDBConfigGenerator(WPGenerateContext context, DBConfigDefinition excelContent) {
        super(context, excelContent);
    }

    public WPDBConfigGenerator(WPGenerateContext context, List<DBConfigDefinition> excelContents) {
        super(context, excelContents);
    }

    @Override
    public String[] getTemplateNames() {
        return new String[] { "dm" };
    }

    @Override
    public Map<String, Object> getReplaceMap(DBConfigDefinition excelContent) {
        List<DBConfigItemDefinition> processList = excelContent.getProcessList();
        this.logPrefix = String.format("[%s]", excelContent.getSheetName());
        if (processList == null || processList.isEmpty()) {
            writeWarnLog("データモデルの操作が０件、処理スキップ");
            return null;
        }
        String dataModelLogicName = processList.get(0).getDataModel();
        if (StringUtils.isEmpty(dataModelLogicName)) {
            writeErrorLog("「データモデル」未設定、処理スキップ");
            return null;
        }
        TableBean tableBean = context.getTableSearchService().findTableByFullName(dataModelLogicName);
        if (tableBean == null) {
            writeErrorLog("テーブル定義が見つからない、データモデルの操作をスキップ: " + dataModelLogicName);
            return null;
        }

        Map<String, Object> replaceMap = new HashMap<String, Object>();
        replaceMap.put("dmId", tableBean.getTableName());
        replaceMap.put("dmName", escapseXml(tableBean.getTableFullName()));
        List<DmItem> dmList = WPCommonDMGenerator.createDmItemList(tableBean);
        replaceMap.put("dmItemList", dmList);

        List<DmOpe> dmOpeList = new ArrayList<>();
        for (DBConfigItemDefinition config : processList) {
            if (StringUtils.isEmpty(config.getDataModel())) {
                writeWarnLog("「データモデル」未設定，スキップ");
                continue;
            }
            if (!StringUtils.equals(dataModelLogicName, config.getDataModel())) {
                writeErrorLog("同一シート内で「データモデル」が異なる行が存在する、処理スキップ: " + config.getDataModel());
                continue;
            }
            this.logSubPrefix = String.format("[%s:%s]", config.getDataModel(), config.getOperationCode());
            DmOpe dmOpe = new DmOpe();
            dmOpe.code = config.getOperationCode();
            dmOpe.name = escapseXml(config.getTableName());
            if ("登録".equalsIgnoreCase(config.getOperation())) {
                dmOpe.type = "INSERT";
            } else if ("更新".equalsIgnoreCase(config.getOperation())) {
                dmOpe.type = "UPDATE";
            } else {
                writeWarnLog("unknown 操作: " + config.getOperation());
            }

            List<DBConfigSubItemDefinition> subItems = config.getDetails();
            List<DmOpeLogic> logicList = new ArrayList<DmOpeLogic>();
            for (DBConfigSubItemDefinition subItem : subItems) {
                this.logSubPrefix = String.format("[%s:%s:%s]", config.getDataModel(), config.getOperationCode(), subItem.getLogicalName());
                DmOpeLogic dmOpeLogic = new DmOpeLogic();
                dmOpeLogic.item_code = subItem.getPhysicalName();
                String statement = subItem.getConfigContent();
                if (statement != null) {
                    statement = convertZenToHen(statement, "設定内容");
                    // 画面.xxxxx 和 パラメータ.xxxxx 和 xxxx.xxxx 置换成IN.ITEM
                    if (statement.contains("画面") || statement.contains("パラメータ")) {
                        statement = statement.replace("画面." + subItem.getLogicalName(), "_IN_._ITEM_");
                        statement = statement.replace("画面．" + subItem.getLogicalName(), "_IN_._ITEM_");
                        statement = statement.replace("パラメータ．" + subItem.getLogicalName(), "_IN_._ITEM_");
                        statement = statement.replace("パラメータ．" + subItem.getLogicalName(), "_IN_._ITEM_");
                    }
                } else {
                    writeErrorLog("「設定内容」未設定");
                }
                dmOpeLogic.statement = escapseXml(statement);
                String description = subItem.getLogicalName() + " : " + subItem.getConfigContent();
                if (StringUtils.isNotEmpty(subItem.getRemarks())) {
                    description += " (" + subItem.getRemarks() + ")";
                }
                dmOpeLogic.description = escapseXml(description);
                logicList.add(dmOpeLogic);
            }
            dmOpe.logicList = logicList;
            dmOpeList.add(dmOpe);
        }
        replaceMap.put("dmOpeList", dmOpeList);
        return replaceMap;
    }

}
