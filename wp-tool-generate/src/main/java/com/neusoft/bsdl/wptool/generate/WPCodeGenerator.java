package com.neusoft.bsdl.wptool.generate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.text.StringEscapeUtils;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.core.exception.WPException;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescription;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescriptionResult;
import com.neusoft.bsdl.wptool.generate.model.ChoiceBean;
import com.neusoft.bsdl.wptool.generate.model.IOItem;
import com.neusoft.bsdl.wptool.generate.model.ItemProp;

import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.morphem.MorphemHelper;
import cbai.util.template.CreateTemplateVelocity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WPCodeGenerator {
    private final static String[][] ACTION_ARRAY = { { "検索", "SEARCH" }, { "登録", "INSERT" }, { "更新", "UPDATE" }, { "登録（更新）", "UPDATE" }, { "削除", "DELETE" }, { "新規", "CREATE" }, { "選択", "CHOICE" },
            { "クリア", "CLEAR" }, { "次へ", "NEXT" }, { "一覧画面へ", "LIST" }, { "戻る", "BACK" }, { "閉じる", "CLOSE" }, { "インポート", "IMPORT" }, { "エクスポート", "EXPORT" }, { "帳票出力", "EXPORT" }, { "追加", "ADDITION" },
            { "○○追加", "ADDITION" }, { "編集", "EDIT" }, { "確認", "PRE_PROPOSE" }, { "申請", "PROPOSE" }, { "○○申請", "PROPOSE" }, { "コピー", "COPY" }, { "複写", "COPY" }, { "失注", "LOST" }, { "再計算" },
            { "RECALCULATION" }, { "引当要求", "RESERVE" }, { "照会", "QUERY" }, { "○○照会", "QUERY" }, { "状況", "STATUS" }, { "受領", "RECEIPT" }, { "○○受領", "RECEIPT" }, { "保管", "STORAGE" },
            { "○○保管", "STORAGE" }, { "作業指示", "WORK" }, { "出荷指示", "SHIPPING" } };

    private final static CreateTemplateVelocity createTemplate = new CreateTemplateVelocity("com.neusoft.bsdl.wptool.generate.WP_TEMPLATE", true);
    private WPContext context;
    private String logPrefix;
    private static MorphemHelper morphemHelper = null;
    static {
//        helper = new MorphemHelper(WPDictCreator.getDictList());
        // TODO 做成项目用词典
        morphemHelper = new MorphemHelper();
    }

    public WPCodeGenerator(WPContext context) {
        this.context = context;
    }

    public void generate(ScreenExcelContent screenExcelContent, File outputDir) throws IOException {
        createIO(screenExcelContent, outputDir);
    }

    public void createIO(ScreenExcelContent screenExcelContent, File outputDir) throws IOException {
        createTemplate.create(outputDir, getIoReplaceMap(screenExcelContent, "IO"), "io");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getIoReplaceMap(ScreenExcelContent screenExcelContent, String ioType) {
        Map<String, Object> replaceMap = new HashMap<String, Object>();
        String ioSuffix = "";
        String subLogPrefix = "";
        ExcelSheetContent<List<ScreenItemDescriptionResult>> result = (ExcelSheetContent<List<ScreenItemDescriptionResult>>) screenExcelContent.getSheetList().stream()
                .filter(s -> "画面項目説明書".equals(s.getSheetName())).findFirst().orElse(null);
        if (result == null) {
            throw new WPException("画面項目説明書シートが見つかりません");
        }
        List<ScreenItemDescriptionResult> list = null;
        if ("IO".equals(ioType)) {
            ioSuffix = "IO";
            list = result.getContent();
            subLogPrefix = "画面項目説明書 ";
        } else if ("EXPORT".equals(ioType)) {
            ioSuffix = "EX";
            subLogPrefix = "画面項目説明書 (エクスポート) ";
            throw new WPException("エクスポートIOの生成はまだ実装されていません");
        } else {
            throw new WPException("不明なIOタイプ: " + ioType);
        }
        this.logPrefix = String.format("[%s:%s] ", screenExcelContent.getScreenId(), screenExcelContent.getScreenName());

        replaceMap.put("io_type", ioType);
        replaceMap.put("gmId", screenExcelContent.getScreenId());
        replaceMap.put("gmIoId", screenExcelContent.getScreenId() + ioSuffix);
        replaceMap.put("gmName", screenExcelContent.getScreenName());
//        replaceMap.put("parentDir", screenExcelContent.getScreenId());
//        SR対象条件Bean dxtjBean = xxsjBean.対象条件;
//        if (dxtjBean != null) {
//            if (StringUtils.isNotEmpty(dxtjBean.対象データモデル)) {
//                String[] array = dxtjBean.対象データモデル.split(" +");
//                String gmDmName = array[0];
//                String gmDmCode = "";
//                if (array.length > 1) {
//                    gmDmCode = array[1];
//                } else {
//                    FindedTableItem findedTableItem = sqlConvert.findTableItem(gmDmName, null);
//                    if (findedTableItem.isFind()) {
//                        TableBean tb = findedTableItem.getTableBean();
//                        gmDmCode = tb.getTableName();
//                    } else {
//                        writeErrorLog(subLogPrefix + "対象条件DMが見つかりません:{}", gmDmName);
//                    }
//                }
//                replaceMap.put("gmDmCode", gmDmCode);
//                String gmIoCondition = dxtjBean.対象条件;
//                if (StringUtils.isNotEmpty(gmIoCondition)) {
//                    gmIoCondition = gmIoCondition.replace("(完全一致)", "");
//                    gmIoCondition = gmIoCondition.replace("パラメータ @", "@");
//                    gmIoCondition = gmIoCondition.replace("＝", "=");
//                    gmIoCondition = gmIoCondition.replace("かつ", " and ");
//                    gmIoCondition = gmIoCondition.replace("　", " ");
//                    gmIoCondition = gmIoCondition.replace("’", "'");
//                    gmIoCondition = gmIoCondition.replace("’", "'");
//                    gmIoCondition = gmIoCondition.replace("\n", " ");
//                    Matcher m = CONDTION_PATTERN.matcher(gmIoCondition);
//                    StringBuffer sb = new StringBuffer();
//                    while (m.find()) {
//                        String filedFullName = m.group(1).trim();
//                        FieldBean fb = sqlConvert.getFieldFromDBName(gmDmName, filedFullName);
//                        if (fb == null) {
//                            writeErrorLog(subLogPrefix + "対象条件が見つかりません:{}.{}", gmDmName, filedFullName);
//                            continue;
//                        }
//                        m.appendReplacement(sb, fb.getFieldName() + "=");
//                    }
//                    m.appendTail(sb);
//                    gmIoCondition = sb.toString().trim();
//                }
//                replaceMap.put("gmIoCondition", escapseXml(gmIoCondition));
//            }
//        }
        Map<String, List<IOItem>> itemNameMap = new HashMap<String, List<IOItem>>();
        Set<String> codeSet = new HashSet<String>();
        List<IOItem> ioItemList = new ArrayList<>();
        int groupIndex = 0;
        boolean isInGroup = false;
        String curGroupPrefix = "";
        for (ScreenItemDescriptionResult itemGroup : list) {
            curGroupPrefix = "";
            isInGroup = false;
            for (ScreenItemDescription itemBean : itemGroup.getItems()) {
                IOItem ioItem = new IOItem();
                ioItem.io_code = screenExcelContent.getScreenId();
                ioItem.name = itemBean.getItemName();// itemBean.getItemName();
                String display = itemBean.getDisplay();// itemBean.表示;
                String io = itemBean.getIo();// itemBean.IO;
                String codePrefix = "D_";
                if (io.contains("I入力")) {
                    ioItem.item_type = "I";
//              codePrefix = "I_";
                } else if (io.contains("IO入出力")) {
                    ioItem.item_type = "IO";
//              codePrefix = "IO_";
                } else if (io.contains("O出力")) {
                    ioItem.item_type = "O";
//              codePrefix = "O_";
                } else if (io.contains("Aアクション")) {
                    ioItem.item_type = "A";
                    codePrefix = "A_";
                } else if (io.contains("Gグループ")) {
                    ioItem.level = "1";
                    ioItem.item_type = "G";
                    groupIndex++;
                    isInGroup = true;
                    codePrefix = "G" + groupIndex + "_";
                    curGroupPrefix = "G" + groupIndex + "_";
                } else {
                    writeErrorLog(subLogPrefix + "項番[{}] unkonw I/O :{}", itemBean.getItemNo(), io);
                }
                if (display != null && display.contains("非表示")) {
                    ioItem.is_visible = "false";
                    codePrefix = "H_";
                } else {
                    ioItem.is_visible = "true";
                }
                if (isInGroup && !io.contains("Gグループ")) {
                    codePrefix = curGroupPrefix + codePrefix;
                    ioItem.level = "2";
                }
                if ("DM".equalsIgnoreCase(itemBean.getLengthWP())) {
                    // DM不设置【桁数】，使用默认值
                } else if (itemBean.getLengthWP().matches("\\d+")) {
                    ioItem.length = itemBean.getLengthWP();
                } else {
                    writeWarnLog(subLogPrefix + "項番[{}] unkonw 桁数(WP) :{}", itemBean.getItemNo(), itemBean.getLengthWP());
                }

                // 備考
                setBiko(itemBean, ioItem);

                if (hasValue(itemBean.getModelName()) /* && !tableFullName.endsWith("クエリ") */) {
                    // 対象テーブル情報
                    String tableFullName = itemBean.getModelName().replaceAll("[\r\n]", "");
                    String fieldFullName = itemBean.getModelItemName().replaceAll("[\r\n]", "");
                    TableBean tb = context.getTableSearchService().findTableByFullName(tableFullName);
                    if (tb != null) {
                        if ("DM".equals(itemBean.getAttributeWP())) {
                            ioItem.dm_code = tb.getTableName();
                        }
                        FieldBean fb = context.getTableSearchService().findFieldByFullName(tableFullName, fieldFullName);
                        if (fb != null) {
                            if ("DM".equals(itemBean.getAttributeWP())) {
                                ioItem.dm_item_code = fb.getFieldName();
                            }
                            ioItem.code = fb.getFieldName();
                        } else {
                            ioItem.dm_item_code = fieldFullName;
                            writeErrorLog(subLogPrefix + "項番[{}] テーブル項目が見つかりません:{}.{}", itemBean.getItemNo(), tableFullName, fieldFullName);
                        }
                    } else {
                        ioItem.dm_code = tableFullName;
                        writeErrorLog(subLogPrefix + "項番[{}] テーブルが見つかりません:{}", itemBean.getItemNo(), tableFullName);
                    }
                } else {
                    if ("A".equals(ioItem.item_type)) {
                        ioItem.code = getActionIoCode(itemBean.getItemName(), ioItem.label);
                    }
                    if (StringUtils.isEmpty(ioItem.code)) {
                        String id = morphemHelper.getRomaFromKanji(itemBean.getItemName()).toUpperCase();
                        ioItem.code = id;
                    }
                }
                if (!"DM".equals(itemBean.getAttributeWP())) {
                    if (StringUtils.isNotEmpty(itemBean.getAttributeWP()) && itemBean.getAttributeWP().length() > 1) {
                        ioItem.dm_item_code = "@" + itemBean.getAttributeWP();
                    } else {
                        if (ioItem.item_type.contains("I") || ioItem.item_type.contains("O")) {
                            ioItem.dm_item_code = "@TEXT";
                        }
                    }
                }
                if (!"G".equals(ioItem.item_type) && StringUtils.isNotEmpty(ioItem.code) /* && !"DM".equals(itemBean.getAttributeWP()) */) {
                    ioItem.code = codePrefix + ioItem.code;
                    while (!codeSet.add(ioItem.code)) {
                        if (ioItem.code.matches(".*_\\d+$")) {
                            try {
                                String part1 = ioItem.code.substring(0, ioItem.code.lastIndexOf("_"));
                                String part2 = ioItem.code.substring(ioItem.code.lastIndexOf("_") + 1);
                                int suffixLen = part2.length();
                                int index = Integer.parseInt(part2);
                                suffixLen = Math.max(suffixLen, String.valueOf(index + 1).length());
                                ioItem.code = part1 + "_" + StringUtils.leftPad(String.valueOf((index + 1)), suffixLen, '0');
                            } catch (Exception e) {
                                ioItem.code = ioItem.code + "_1";
                            }
                        } else {
                            if ("A".equals(ioItem.item_type)) {
                                ioItem.code = ioItem.code + "_01";
                            } else {
                                ioItem.code = ioItem.code + "_1";
                            }
                        }
                        ioItem.code = ioItem.code.replaceAll("_+", "_");
                    }
                }
                // 初期値
                ioItem.default_value = getInitValue(subLogPrefix, itemBean);
                // 加工式
                ioItem.statement = getStatement(subLogPrefix, itemBean);
                // 選択リスト
                ioItem.choiceInfo = getChoiceInfo(subLogPrefix, itemBean);
                // 表示条件
                ioItem.condition = getCondition(subLogPrefix, itemBean);
                ioItemList.add(ioItem);
                if (StringUtils.isNotEmpty(ioItem.name)) {
                    if (!itemNameMap.containsKey(ioItem.name)) {
                        itemNameMap.put(ioItem.name, new ArrayList<IOItem>());
                    }
                    itemNameMap.get(ioItem.name).add(ioItem);
                }
            }
        }
//        if (xxsjBean.画面チェック仕様書 != null && xxsjBean.画面チェック仕様書.listチェックItemMap != null) {
//            Map<String, Integer> actionIndexMap = new HashMap<String, Integer>();
//            xxsjBean.画面チェック仕様書.listチェックItemMap.forEach((key, value) -> {
//                if (key.contains("IO")) {
//                    value.forEach((checkItem) -> {
//                        IOItem ioItem = new IOItem();
//                        ioItem.io_code = xxsjBean.画面ID;
//                        ioItem.name = escapseXml(checkItem.チェックアクション.trim().replace("\n", "／") + " " + checkItem.チェック名);
//                        ioItem.is_visible = "false";
//                        ioItem.item_type = "C";
//                        String codePrefix = "C_";
//                        String[] actions = checkItem.チェックアクション.trim().split("\n");
////                      if (actions.length == 1 && itemNameMap.containsKey(actions[0])) {
////                          String action = actions[0];
////                          if (!actionIndexMap.containsKey(action)) {
////                              actionIndexMap.put(action, 0);
////                          }
////                          List<IOItem> actionItems = itemNameMap.get(action);
////                          for (IOItem item : actionItems) {
////                              if ("A".equals(item.item_type) && item.code.startsWith("A_")) {
////                                  int index = actionIndexMap.get(action) + 1;
////                                  String code = item.code.substring(2) + "_"
////                                          + StringUtils.leftPad(String.valueOf(index), 2, '0');
////                                  actionIndexMap.put(action, index);
////                                  ioItem.code = code;
////                                  break;
////                              }
////                          }
////                      } else
//                        StringBuilder actionConditionSb = new StringBuilder();
//                        if (actions.length > 0) {
//                            String actionKey = StringUtils.join(actions, "_");
//                            if (!actionIndexMap.containsKey(actionKey)) {
//                                actionIndexMap.put(actionKey, 0);
//                            }
//                            StringBuilder sb = new StringBuilder();
//                            for (String action : actions) {
//                                List<IOItem> actionItems = itemNameMap.get(action);
//                                String name = "";
//                                if (actionItems != null) {
//                                    for (IOItem item : actionItems) {
//                                        if ("A".equals(item.item_type) && item.code != null && item.code.startsWith("A_")) {
//                                            name = item.code.substring(2);
//                                            break;
//                                        }
//                                    }
//                                }
//                                if (sb.length() > 0) {
//                                    sb.append("_");
//                                }
//                                if (StringUtils.isEmpty(name)) {
//                                    name = action;
//                                }
//                                sb.append(name);
//                                if (actionConditionSb.length() > 0) {
//                                    actionConditionSb.append(" OR ");
//                                }
//                                actionConditionSb.append(String.format("@ACTION = '%s'", "A_" + name));
//                            }
//                            String code = sb.toString();
//                            int index = actionIndexMap.get(actionKey) + 1;
//                            code = code + "_" + StringUtils.leftPad(String.valueOf(index), 2, '0');
//                            actionIndexMap.put(actionKey, index);
//                            ioItem.code = code;
//                        }
//                        if (StringUtils.isEmpty(ioItem.code)) {
//                            String action = "IO";
//                            if (!actionIndexMap.containsKey(action)) {
//                                actionIndexMap.put(action, 0);
//                            }
//                            int index = actionIndexMap.get(action) + 1;
//                            String code = action + "_" + StringUtils.leftPad(String.valueOf(index), 2, '0');
//                            actionIndexMap.put(action, index);
//                            ioItem.code = code;
//                        }
//                        ioItem.code = codePrefix + ioItem.code;
//                        if (StringUtils.isNotEmpty(checkItem.メッセージID)) {
//                            ioItem.msg_code_ng = checkItem.メッセージID;
//                        }
//                        ioItem.description = escapseXml(checkItem.仕様説明);
//                        StringBuilder sb = new StringBuilder();
//                        sb.append(String.format("IF( %s ,\n", actionConditionSb.toString()));
//                        if (checkItem.仕様説明.contains("<チェック条件>")) {
//                            String checkCondtion = checkItem.仕様説明.substring(checkItem.仕様説明.indexOf("<チェック条件>") + "<チェック条件>".length());
//                            checkCondtion = checkCondtion.replace("\n", " ");
//                            sb.append(String.format("      IF((%s),\n", checkCondtion));
//                        } else if (checkItem.仕様説明.contains("(＜チェック条件＞")) {
//                            String checkCondtion = checkItem.仕様説明.substring(checkItem.仕様説明.indexOf("(＜チェック条件＞") + "(＜チェック条件＞".length());
//                            checkCondtion = checkCondtion.replace("\n", " ");
//                            sb.append(String.format("      IF((%s),\n", checkCondtion));
//                        } else {
//                            sb.append(String.format("      IF((%s),\n", "XXXXXXXX"));
//                        }
//                        sb.append(String.format("          @FALSE,\n"));
//                        sb.append(String.format("          @TRUE),\n"));
//                        sb.append(String.format("@TRUE)"));
//                        ioItem.condition = escapseXml(sb.toString());
//                        ioItem.is_disable = "true";
//                        ioItemList.add(ioItem);
//                    });
//                }
//            });
//        }
        replaceMap.put("ioItemList", ioItemList);
        return replaceMap;
    }

    private boolean hasValue(String value) {
        return StringUtils.isNotEmpty(value) && !"－".equals(value) && !"ー".equals(value) && !"-".equals(value);
    }

    private String escapseXml(String xml) {
        return StringEscapeUtils.escapeXml11(xml);
    }

    private void writeErrorLog(String format, Object... arguments) {
        log.error(logPrefix + format, arguments);
    }

    private void writeWarnLog(String format, Object... arguments) {
        log.warn(logPrefix + format, arguments);
//        if (logger != null) {
//            logger.warn(logPrefix + format, arguments);
//        }
    }

    private String getActionIoCode(final String name, final String label) {
        String[] texts = new String[] { name, label };
        for (String text : texts) {
            if (StringUtils.isEmpty(text)) {
                continue;
            }
            for (String[] actionInfo : ACTION_ARRAY) {
                if (text.equals(actionInfo[0])) {
                    return (String) actionInfo[1];
                } else if (actionInfo[0].startsWith("○○")) {
                    if (text.endsWith(actionInfo[0].substring(2))) {
                        return (String) actionInfo[1];
                    }
                } else if (actionInfo[0].endsWith("○○")) {
                    if (text.startsWith(actionInfo[0].substring(0, actionInfo[0].length() - 2))) {
                        return (String) actionInfo[1];
                    }
                }
            }
        }
        return null;
    }

    private void setBiko(ScreenItemDescription itemBean, IOItem ioItem) {
        String bikoText = itemBean.getRemarks();// itemBean.備考;
        if (!hasValue(bikoText)) {
            return;
        }
        String[] bikoList = bikoText.split("\n");
        for (String biko : bikoList) {
//            if (biko.startsWith("fieldType")) {
//                biko = biko.replace("：", ":");
//                if (biko.indexOf(":") != -1) {
//                    String value = biko.substring(biko.indexOf(":") + 1);
//                    if (ioItem.io_item_prop_list == null) {
//                        ioItem.io_item_prop_list = new ArrayList<ItemProp>();
//                    }
//                    ioItem.io_item_prop_list.add(new ItemProp("fieldType", escapseXml(value)));
//                }
//            } else if (biko.startsWith("defaultFieldStyle")) {
//                biko = biko.replace("：", ":");
//                if (biko.indexOf(":") != -1) {
//                    String value = biko.substring(biko.indexOf(":") + 1);
//                    if (ioItem.io_item_prop_list == null) {
//                        ioItem.io_item_prop_list = new ArrayList<ItemProp>();
//                    }
//                    ioItem.io_item_prop_list.add(new ItemProp("defaultFieldStyle", escapseXml(value)));
//                }
//            }            else 
            if (biko.startsWith("ラベル付加")) {
                biko = biko.replace("：", ":");
                if (biko.indexOf(":") != -1) {
                    String value = biko.substring(biko.indexOf(":") + 1);
                    if (ioItem.io_item_prop_list == null) {
                        ioItem.io_item_prop_list = new ArrayList<ItemProp>();
                    }
                    // ラベル付加
                    ioItem.io_item_prop_list.add(new ItemProp("labelAvailable", escapseXml(value)));
                }
            } else if (biko.startsWith("ラベル式")) {
                biko = biko.replace("：", ":");
                if (biko.indexOf(":") != -1) {
                    String value = biko.substring(biko.indexOf(":") + 1);
                    if (ioItem.io_item_prop_list == null) {
                        ioItem.io_item_prop_list = new ArrayList<ItemProp>();
                    }
                    // TODO: ラベル式：画面.汎用CD見出し
                    // （未設定時、"汎用コード"を表示）
                    ioItem.io_item_prop_list.add(new ItemProp("labelStatement", escapseXml(value)));
                }
            } else {
                writeWarnLog(logPrefix + "項番[{}] unkonw 備考:{}", itemBean.getItemNo(), biko);
            }
        }
        ioItem.description = escapseXml(bikoText);
    }

    private static final Pattern CONDTION_PATTERN = Pattern.compile("([^　 =]+) *=");

    private String getCondition(String subLogPrefix, ScreenItemDescription itemBean) {
        // 表示条件
        String condition = itemBean.getDisplayCondition();
        if (!hasValue(condition)) {
            return null;
        }
        return escapseXml(condition);
    }

    private String getInitValue(String subLogPrefix, ScreenItemDescription itemBean) {
        String initValue = itemBean.getDefaultValue();
        if (!hasValue(initValue)) {
            return null;
        }
//        if (initValue.startsWith("受取パラメータ")) {
//            initValue = initValue.substring("受取パラメータ".length()).replaceAll("　", " ").trim();
//        } else if (initValue.startsWith("@") && initValue.contains("パラメータ")) {
//            Matcher m = PARAM_PATTERN.matcher(initValue);
//            if (m.find()) {
//                initValue = m.group();
//            }
//        } 
        if (initValue.startsWith("固定値：") || initValue.startsWith("固定値:")) {
            initValue = initValue.replaceAll("　", " ").trim();
            initValue = "'" + initValue.substring(4).trim() + "'";
        }
        return escapseXml(initValue);
    }

    private String getStatement(String subLogPrefix, ScreenItemDescription itemBean) {
        // 加工式
        String statement = itemBean.getProcessingRule();
        if (!hasValue(statement)) {
            return null;
        }
        Map<String, List<String>> map = parseDefineCell(itemBean, statement);
        if (!map.isEmpty()) {
            if (map.containsKey("DM")) {
                List<String> dmList = map.get("DM");
                List<String> conditionList = map.get("条件");
                List<String> valueList = map.get("値");

                if (dmList == null || dmList.isEmpty()) {
                    writeWarnLog(subLogPrefix + "項番[{}] 加工式の【DM】が見つかりません", itemBean.getItemNo());
                    return "'" + escapseXml(statement) + "'";
                }
                String dm = dmList.get(0);

                TableBean findedTableItem = context.getTableSearchService().findTableByFullName(dm);
                if (findedTableItem == null) {
                    writeErrorLog(subLogPrefix + "項番[{}] 加工式の【DM】が見つかりません:{}", itemBean.getItemNo(), dm);
                    return "'" + escapseXml(statement) + "'";
                }
                String dmCodeResult = findedTableItem.getTableName();
                String conditionResult = "";
                if (conditionList == null || conditionList.isEmpty()) {
                    writeErrorLog(subLogPrefix + "項番[{}] 加工式の【条件】が見つかりません", itemBean.getItemNo());
                } else {
                    StringBuffer sb = new StringBuffer();
                    for (String condition : conditionList) {
                        condition = condition.replace("＝", "=");
                        condition = condition.replace("かつ", " and ");
                        condition = condition.replace("　", " ");
                        condition = condition.replace("’", "'");
                        sb.append(condition).append(" ");
                    }
                    Matcher m = CONDTION_PATTERN.matcher(sb.toString().trim());
                    sb.setLength(0);
                    while (m.find()) {
                        String filedFullName = m.group(1).trim();
                        FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), filedFullName);
                        if (fb == null) {
                            writeErrorLog(subLogPrefix + "項番[{}] 加工式の【条件】が見つかりません:{}", itemBean.getItemNo(), filedFullName);
                            continue;
                        }
                        m.appendReplacement(sb, fb.getFieldName() + "=");
                    }
                    m.appendTail(sb);
                    conditionResult = sb.toString();
                }
                String valueResult = "";
                if (valueList == null || valueList.isEmpty()) {
                    writeErrorLog(subLogPrefix + "項番[{}] 加工式の【値】が見つかりません", itemBean.getItemNo());
                } else {
                    String value = valueList.get(0);
                    FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), value);
                    if (fb == null) {
                        writeErrorLog(subLogPrefix + "項番[{}] 加工式の【値】が見つかりません:{}", itemBean.getItemNo(), value);
                    } else {
                        valueResult = fb.getFieldName();
                    }
                }
                String newStatement = String.format("NAVAL(%s{%s}.%s, @NULL)", dmCodeResult, conditionResult, valueResult);
                return escapseXml(newStatement);
            } else if (map.containsKey("条件") && map.containsKey("設定値")) {
                // TODO
                /*
                 * [条件] 画面.契約締結年月＜ 画面.業務日付-6ヶ月(年月) [設定値] true：契約締結年月に6ヶ月以上前の過去日付が指定されています。
                 * false：@NULL
                 */
            }
        }
        if (statement.startsWith("@READONLY") || statement.startsWith("読み取り専用")) {
            return "@READONLY";
        }
        return "'" + escapseXml(statement) + "'";
    }

    private ChoiceBean getChoiceInfo(String subLogPrefix, ScreenItemDescription itemBean) {
        String selectList = itemBean.getSelectList();// itemBean.選択リスト;
        if (!hasValue(selectList)) {
            return null;
        }
        ChoiceBean choiceInfo = null;
        Map<String, List<String>> map = parseDefineCell(itemBean, selectList);
        if (map.isEmpty()) {
            if (StringUtils.isNotEmpty(selectList)) {
                choiceInfo = new ChoiceBean();
                choiceInfo.fixed = escapseXml(selectList);
            }
            return choiceInfo;
        }
        List<String> fixedList = map.get("固定値");
        if (fixedList != null && !fixedList.isEmpty()) {
            choiceInfo = new ChoiceBean();
            choiceInfo.fixed = fixedList.get(0);
        }
        List<String> dmList = map.get("DM");
        List<String> conditionList = map.get("条件");
        List<String> valueList = map.get("値");
        List<String> labelList = map.get("名称");
        List<String> orderList = map.get("表示順");
        if (dmList == null || dmList.isEmpty()) {
            writeWarnLog(subLogPrefix + "項番[{}] 選択リストの【DM】が見つかりません", itemBean.getItemNo());
            return choiceInfo;
        }
        String dm = dmList.get(0);

        TableBean findedTableItem = context.getTableSearchService().findTableByFullName(dm);
        if (findedTableItem == null) {
            writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【DM】が見つかりません:{}", itemBean.getItemNo(), dm);
            return choiceInfo;
        }
        if (choiceInfo == null) {
            choiceInfo = new ChoiceBean();
        }
        choiceInfo.dmCode = findedTableItem.getTableName();
        if (conditionList == null || conditionList.isEmpty()) {
            writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【条件】が見つかりません", itemBean.getItemNo());
        } else {
            StringBuffer sb = new StringBuffer();
            for (String condition : conditionList) {
                condition = condition.replace("＝", "=");
                condition = condition.replace("かつ", " and ");
                condition = condition.replace("　", " ");
                condition = condition.replace("’", "'");
                sb.append(condition).append(" ");
            }
            Matcher m = CONDTION_PATTERN.matcher(sb.toString().trim());
            sb.setLength(0);
            while (m.find()) {
                String filedFullName = m.group(1).trim();
                FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), filedFullName);
                if (fb == null) {
                    writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【条件】が見つかりません:{}", itemBean.getItemNo(), filedFullName);
                    continue;
                }
                m.appendReplacement(sb, fb.getFieldName() + "=");
            }
            m.appendTail(sb);
            choiceInfo.condition = sb.toString();
        }
        if (valueList == null || valueList.isEmpty()) {
            writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【値】が見つかりません", itemBean.getItemNo());
        } else {
            String value = valueList.get(0);
            FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), value);
            if (fb == null) {
                writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【値】が見つかりません:{}", itemBean.getItemNo(), value);
            } else {
                choiceInfo.valueDmItemCode = fb.getFieldName();
            }
        }
        if (labelList == null || labelList.isEmpty()) {
            writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【名称】が見つかりません", itemBean.getItemNo());
        } else {
            for (int i = 0; i < labelList.size(); i++) {
                String label = labelList.get(i);
                FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), label);
                if (fb == null) {
                    writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【名称】が見つかりません:{}", itemBean.getItemNo(), label);
                } else {
                    try {
                        BeanUtils.setProperty(choiceInfo, "nameDmItemCode" + (i + 1), fb.getFieldName());
                    } catch (Exception e) {
                        writeErrorLog(subLogPrefix + "項番[{}] 選択リスのの【名称】設定エラー", itemBean.getItemNo(), label, e);
                    }
                }
            }
        }

        if (orderList == null || orderList.isEmpty()) {
            writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【表示順】が見つかりません", itemBean.getItemNo());
        } else {
            for (int i = 0; i < orderList.size(); i++) {
                String order = orderList.get(i);
                String orderType = null;
                if (order.contains("昇順")) {
                    orderType = "A";
                } else {
                    orderType = "D";
                }
                String orderLabel = order.replace("（昇順）", "").replace("（降順）", "").trim();
                FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), orderLabel);
                if (fb == null) {
                    writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【表示順】が見つかりません:{}", itemBean.getItemNo(), order);
                } else {
                    try {
                        BeanUtils.setProperty(choiceInfo, "sortDmItemCode" + (i + 1), fb.getFieldName());
                    } catch (Exception e) {
                        writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【表示順】設定エラー", itemBean.getItemNo(), order, e);
                    }
                    try {
                        BeanUtils.setProperty(choiceInfo, "sortType" + (i + 1), orderType);
                    } catch (Exception e) {
                        writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【表示順】設定エラー", itemBean.getItemNo(), order, e);
                    }
                }
            }
        }
        return choiceInfo;
    }

    private Map<String, List<String>> parseDefineCell(ScreenItemDescription itemBean, String defineValue) {
        Map<String, List<String>> map = new LinkedHashMap<String, List<String>>();

        String[] lines = defineValue.trim().split("\n");
        List<String> curList = null;
        for (String line : lines) {
            if (line.startsWith("[")) {
                line = line.substring(1);
                if (line.indexOf("]") == -1) {
                    writeErrorLog("項番[{}] 選択リストの解析失敗、「】」が見つかりません:{}", itemBean.getItemNo(), defineValue);
                    return map;
                }
                String key = line.substring(0, line.indexOf("]"));
                curList = new ArrayList<String>();
                map.put(key, curList);
                line = line.substring(line.indexOf("]") + 1).trim();
            }
            if (curList != null) {
                line = line.trim();
                if (!StringUtils.isEmpty(line)) {
                    curList.add(line);
                }
            } else {
                continue;
            }
        }
        return map;
    }

}
