package com.neusoft.bsdl.wptool.generate;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtils;

import com.neusoft.bsdl.wptool.core.exception.WPException;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.model.ScreenDefinition;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescription;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescriptionResult;
import com.neusoft.bsdl.wptool.core.model.ScreenValidation;
import com.neusoft.bsdl.wptool.core.model.ScreenValidationAction;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;
import com.neusoft.bsdl.wptool.generate.model.ChoiceBean;
import com.neusoft.bsdl.wptool.generate.model.IOItem;
import com.neusoft.bsdl.wptool.generate.model.ItemProp;
import com.neusoft.bsdl.wptool.generate.model.ItemPropMapping;

import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WPIOGenerator extends WPAbstractGenerator<ScreenExcelContent> {
//    private final static String[][] ACTION_ARRAY = { { "検索", "SEARCH" }, { "登録", "INSERT" }, { "更新", "UPDATE" }, { "登録（更新）", "UPDATE" }, { "削除", "DELETE" }, { "新規", "CREATE" }, { "選択", "CHOICE" },
//            { "クリア", "CLEAR" }, { "次へ", "NEXT" }, { "一覧画面へ", "LIST" }, { "戻る", "BACK" }, { "閉じる", "CLOSE" }, { "インポート", "IMPORT" }, { "エクスポート", "EXPORT" }, { "帳票出力", "EXPORT" }, { "追加", "ADDITION" },
//            { "○○追加", "ADDITION" }, { "編集", "EDIT" }, { "確認", "PRE_PROPOSE" }, { "申請", "PROPOSE" }, { "○○申請", "PROPOSE" }, { "コピー", "COPY" }, { "複写", "COPY" }, { "失注", "LOST" }, { "再計算" },
//            { "RECALCULATION" }, { "引当要求", "RESERVE" }, { "照会", "QUERY" }, { "○○照会", "QUERY" }, { "状況", "STATUS" }, { "受領", "RECEIPT" }, { "○○受領", "RECEIPT" }, { "保管", "STORAGE" },
//            { "○○保管", "STORAGE" }, { "作業指示", "WORK" }, { "出荷指示", "SHIPPING" } };

    public enum IOType {
        IO, EXPORT
    }

    /**
     * IOタイプ（IO/EXPORT）
     */
    private IOType ioType = IOType.IO;

    public WPIOGenerator(WPGenerateContext context) {
        super(context);
    }

    public WPIOGenerator(WPGenerateContext context, IOType ioType) {
        super(context);
        this.ioType = ioType == null ? IOType.IO : ioType;
    }

    @Override
    public String[] getTemplateNames() {
        return new String[] { "io" };
    }

    private static final Pattern[] GM_ITEM_PATTERN_LIST = new Pattern[] { Pattern.compile("[\\S&&[^.]]+\\.[\\S&&[^.]]+"), Pattern.compile("[\\S&&[^.,=><]]+\\.[\\S&&[^.,=<>]]+"),
            Pattern.compile("[\\S&&[^(.,=><]]+\\.[\\S&&[^).,=<>]]+") };

    private String convertGmName2Id(Map<String, List<IOItem>> itemNameMap, String content) {
        content = content.replaceAll("\t", " ");
        content = content.replaceAll("．", ".");
        content = content.replaceAll("　", " ");
        content = content.replaceAll("＜", "<");
        content = content.replaceAll("＞", ">");
        content = content.replaceAll("＝", "=");
        for (Pattern pattern : GM_ITEM_PATTERN_LIST) {
            Matcher matcher = pattern.matcher(content);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String item = matcher.group().trim();
                String[] temp = item.split("\\.");
                if (!temp[0].equals("画面")) {
                    continue;
                }
                System.out.println(item);
                if (itemNameMap.containsKey(temp[1])) {
                    String code = itemNameMap.get(temp[1]).get(0).code;
                    if (code != null) {
                        matcher.appendReplacement(sb, code);
                    }
                }
            }
            matcher.appendTail(sb);
            content = sb.toString();
        }
        return content;
    }

    public static void main(String[] args) {
        String content = "画面.汎用コード（更新前情報）<>@NULL";
        Pattern[] GM_ITEM_PATTERN_LIST = new Pattern[] { Pattern.compile("[\\S&&[^.]]+\\.[\\S&&[^.]]+"), Pattern.compile("[\\S&&[^.,=><]]+\\.[\\S&&[^.,=<>]]+"),
                Pattern.compile("[\\S&&[^(.,=><]]+\\.[\\S&&[^).,=<>]]+") };
        content = content.replaceAll("\t", " ");
        content = content.replaceAll("．", ".");
        content = content.replaceAll("　", " ");
        content = content.replaceAll("＜", "<");
        content = content.replaceAll("＞", ">");
        content = content.replaceAll("＝", "=");

        for (Pattern pattern : GM_ITEM_PATTERN_LIST) {
            Matcher matcher = pattern.matcher(content);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String item = matcher.group().trim();
                String[] temp = item.split("\\.");
                if (!temp[0].equals("画面")) {
                    continue;
                }
                System.out.println(item);
//                String fullName = getFieldNameFromFullName(temp[0], temp[1], true, aliaseMap);
//                if (fullName.contains("$")) {
//                    fullName = fullName.replace("$", "\\$");
//                }
//                matcher.appendReplacement(sb, "AAAAAAAAAA");
            }
//            matcher.appendTail(sb);
//            content = sb.toString();
        }
//        System.out.println(content);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getReplaceMap(ScreenExcelContent screenExcelContent) {
        Map<String, Object> replaceMap = new HashMap<String, Object>();
        // 画面定義書
        ExcelSheetContent<ScreenDefinition> screenExcelScreenDefinition = (ExcelSheetContent<ScreenDefinition>) screenExcelContent.getSheetList().stream().filter(s -> "画面定義書".equals(s.getSheetName()))
                .findFirst().orElse(null);
        if (screenExcelScreenDefinition != null) {
            this.logPrefix = String.format("[%s:%s %s] ", screenExcelContent.getScreenId(), screenExcelContent.getScreenName(), screenExcelScreenDefinition.getSheetName());
            ScreenDefinition dxtjBean = screenExcelScreenDefinition.getContent();
            if (dxtjBean.getTargetModels() != null && !dxtjBean.getTargetModels().isEmpty()) {
                String gmDmCode = dxtjBean.getTargetModels().get(0).getPhysicalName();
                replaceMap.put("gmDmCode", gmDmCode);
                // 対象条件
                String gmIoCondition = dxtjBean.getTargetCondition();
                if (StringUtils.isNotEmpty(gmIoCondition)) {
                    gmIoCondition = gmIoCondition.replace("(完全一致)", "");
//                    gmIoCondition = gmIoCondition.replace("パラメータ @", "@");
                    gmIoCondition = gmIoCondition.replace("＝", "=");
                    gmIoCondition = gmIoCondition.replace("かつ", " and ");
                    gmIoCondition = gmIoCondition.replace("　", " ");
                    gmIoCondition = gmIoCondition.replaceAll("\\. *", "\\.");
                    gmIoCondition = gmIoCondition.replace("’", "'");
                    gmIoCondition = gmIoCondition.replace("’", "'");
                    gmIoCondition = gmIoCondition.replace("\n", " ");
                    String gmIoConditionConvered = context.getSqlConverter().convert(gmIoCondition);
                    replaceMap.put("gmIoCondition", escapseXml(gmIoConditionConvered));
                }
            }
        }

        // 画面項目説明書
        String ioSuffix = "";
        Map<String, List<IOItem>> itemNameMap = new HashMap<String, List<IOItem>>();
        Set<String> codeSet = new HashSet<String>();
        List<IOItem> ioItemList = new ArrayList<>();
        int groupIndex = 0;
        boolean isInGroup = false;
        String curGroupPrefix = "";
        ExcelSheetContent<List<ScreenItemDescriptionResult>> excelSheetScreenItem = (ExcelSheetContent<List<ScreenItemDescriptionResult>>) screenExcelContent.getSheetList().stream()
                .filter(s -> "画面項目説明書".equals(s.getSheetName())).findFirst().orElse(null);
        if (excelSheetScreenItem == null) {
            throw new WPException("画面項目説明書シートが見つかりません");
        }
        List<ScreenItemDescriptionResult> list = null;
        switch (ioType) {
        case IO:
            ioSuffix = "IO";
            list = excelSheetScreenItem.getContent();
            break;
        case EXPORT:
            ioSuffix = "EX";
            throw new WPException("エクスポートIOの生成はまだ実装されていません");
        default:
            throw new WPException("不明なIOタイプ: " + ioType);
        }
        this.logPrefix = String.format("[%s:%s %s] ", screenExcelContent.getScreenId(), screenExcelContent.getScreenName(), excelSheetScreenItem.getSheetName());

        replaceMap.put("io_type", ioType.name());
        replaceMap.put("gmId", screenExcelContent.getScreenId());
        replaceMap.put("gmIoId", screenExcelContent.getScreenId() + ioSuffix);
        replaceMap.put("gmName", screenExcelContent.getScreenName());
//        replaceMap.put("parentDir", screenExcelContent.getScreenId());
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
                    writeErrorLog("項番[{}] unkonw I/O :{}", itemBean.getItemNo(), io);
                }
                if ("○".equalsIgnoreCase(itemBean.getRequired())) {
                    ioItem.is_require = "true";
                } else {
                    ioItem.is_require = "false";
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
                if (hasValue(itemBean.getLengthWP())) {
                    if ("DM".equalsIgnoreCase(itemBean.getLengthWP())) {
                        // DM不设置【桁数】，使用默认值
                    } else if (itemBean.getLengthWP().matches("\\d+")) {
                        ioItem.length = itemBean.getLengthWP();
                    } else {
                        writeWarnLog("項番[{}] unkonw 桁数(WP) :{}", itemBean.getItemNo(), itemBean.getLengthWP());
                    }
                }

                // ソート順
                if (hasValue(itemBean.getSorted())) {
                    String[] sorted = itemBean.getSorted().split("\n");
                    if (sorted.length == 2) {
                        ioItem.sort_key = sorted[0].trim();
                        String sortType = sorted[1].trim();
                        if ("昇順".equals(sortType)) {
                            ioItem.sort_type = "A";
                        } else if ("降順".equals(sortType)) {
                            ioItem.sort_type = "D";
                        } else {
                            writeWarnLog("項番[{}] ソート順の形式が不正なソートタイプです:{}", itemBean.getItemNo(), sortType);
                        }
                    } else {
                        writeWarnLog("項番[{}] ソート順の形式が不正です:{}", itemBean.getItemNo(), itemBean.getSorted());
                    }
                }

                if (hasValue(itemBean.getFormat())) {
                    ioItem.format = itemBean.getFormat();
                }
                // 備考
                setBiko(itemBean, ioItem, itemNameMap);

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
                            writeErrorLog("項番[{}] テーブル項目が見つかりません:{}.{}", itemBean.getItemNo(), tableFullName, fieldFullName);
                        }
                    } else {
                        ioItem.dm_code = tableFullName;
                        writeErrorLog("項番[{}] テーブルが見つかりません:{}", itemBean.getItemNo(), tableFullName);
                    }
                } else {
//                    if ("A".equals(ioItem.item_type)) {
//                        ioItem.code = getActionIoCode(itemBean.getItemName(), ioItem.label);
//                    }
//                    if (StringUtils.isEmpty(ioItem.code)) {
                    String id = context.getMorphemHelper().getRomaFromKanji(itemBean.getItemName()).toUpperCase();
                    ioItem.code = id;
//                    }
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
                ioItem.default_value = getInitValue(itemBean);
                // 加工式
                ioItem.statement = getStatement(itemBean);
                // 選択リスト
                ioItem.choiceInfo = getChoiceInfo(itemBean);
                // 表示条件
                ioItem.condition = getCondition(itemBean, itemNameMap);
                ioItemList.add(ioItem);
                if (StringUtils.isNotEmpty(ioItem.name)) {
                    if (!itemNameMap.containsKey(ioItem.name)) {
                        itemNameMap.put(ioItem.name, new ArrayList<IOItem>());
                    } else {
                        writeWarnLog("項番[{}] 項目名 '{}' が重複しています。", itemBean.getItemNo(), ioItem.name);
                    }
                    itemNameMap.get(ioItem.name).add(ioItem);
                }
            }
        }

        // 画面チェック仕様書
        ExcelSheetContent<List<ScreenValidation>> excelSheetScreenValidation = (ExcelSheetContent<List<ScreenValidation>>) screenExcelContent.getSheetList().stream()
                .filter(s -> "画面チェック仕様書".equals(s.getSheetName())).findFirst().orElse(null);
        if (excelSheetScreenValidation.getContent() != null && !excelSheetScreenValidation.getContent().isEmpty()) {
            this.logPrefix = String.format("[%s:%s %s] ", screenExcelContent.getScreenId(), screenExcelContent.getScreenName(), excelSheetScreenValidation.getSheetName());

            Map<String, Integer> actionIndexMap = new HashMap<String, Integer>();
            for (ScreenValidation checkItem : excelSheetScreenValidation.getContent()) {
                if ("BP".equalsIgnoreCase(checkItem.getBizWarining())) {
                    // TODO BP Check
                    continue;
                } else if ("ワーニング".equalsIgnoreCase(checkItem.getBizWarining())) {
                    // TODO ワーニング Check ?
                    continue;
                }
                IOItem ioItem = new IOItem();
                ioItem.io_code = screenExcelContent.getScreenId();
//                ioItem.name = escapseXml(checkItem.チェックアクション.trim().replace("\n", "／") + " " + checkItem.チェック名);
                ioItem.name = escapseXml(checkItem.getValidationName());
                ioItem.is_visible = "false";
                ioItem.item_type = "C";
                String codePrefix = "C_";
                List<ScreenValidationAction> actions = checkItem.getValidationActions();
                StringBuilder actionConditionSb = new StringBuilder();
                if (actions.size() > 0) {
                    String actionKey = actions.stream().filter(a -> a.isHasChecked()).map(a -> a.getActionName()).reduce((a, b) -> a + "_" + b).orElse("");
                    if (!actionIndexMap.containsKey(actionKey)) {
                        actionIndexMap.put(actionKey, 0);
                    }
                    StringBuilder sb = new StringBuilder();
                    for (ScreenValidationAction action : actions) {
                        List<IOItem> actionItems = itemNameMap.get(action.getActionName());
                        String name = "";
                        if (actionItems != null) {
                            for (IOItem item : actionItems) {
                                if ("A".equals(item.item_type) && item.code != null && item.code.startsWith("A_")) {
                                    name = item.code.substring(2);
                                    break;
                                }
                            }
                        }
                        if (sb.length() > 0) {
                            sb.append("_");
                        }
                        if (StringUtils.isEmpty(name)) {
                            name = action.getActionName();
                        }
                        sb.append(name);
                        if (actionConditionSb.length() > 0) {
                            actionConditionSb.append(" OR ");
                        }
                        actionConditionSb.append(String.format("@ACTION = '%s'", "A_" + name));
                    }
                    String code = sb.toString();
                    int index = actionIndexMap.get(actionKey) + 1;
                    code = code + "_" + StringUtils.leftPad(String.valueOf(index), 2, '0');
                    actionIndexMap.put(actionKey, index);
                    ioItem.code = code;
                }
                if (StringUtils.isEmpty(ioItem.code)) {
                    String action = "IO";
                    if (!actionIndexMap.containsKey(action)) {
                        actionIndexMap.put(action, 0);
                    }
                    int index = actionIndexMap.get(action) + 1;
                    String code = action + "_" + StringUtils.leftPad(String.valueOf(index), 2, '0');
                    actionIndexMap.put(action, index);
                    ioItem.code = code;
                }
                ioItem.code = codePrefix + ioItem.code;

                if (StringUtils.isNotEmpty(checkItem.getMessageId())) {
                    ioItem.msg_code_ng = checkItem.getMessageId();
                    StringBuilder paramSb = new StringBuilder();
                    for (int i = 1; i <= 5; i++) {
                        String param = null;
                        try {
                            param = BeanUtils.getProperty(checkItem, "parameter" + i);
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            writeErrorLog("Failed to get parameter{} for check item: {}", i, checkItem.getValidationName());
                            break;
                        }
                        if (StringUtils.isEmpty(param)) {
                            break;
                        }
                        if (i > 1) {
                            paramSb.append(",");
                        }
                        if (param.startsWith("画面.") || param.startsWith("画面．")) {
                            String itemName = param.substring(3);
                            List<IOItem> actionItems = itemNameMap.get(itemName);
                            if (actionItems != null && !actionItems.isEmpty()) {
                                param = actionItems.get(0).code;
                            }
                        } else {
                            param = "'" + param + "'";
                        }
                        paramSb.append(param);
                    }
                    if (paramSb.length() > 0) {
                        ioItem.msg_param_ng = escapseXml(paramSb.toString());
                    }
                }
                ioItem.description = escapseXml(checkItem.getValidationRule());
                // TODO: 解析チェック仕様中的语義，转成加工式
//                StringBuilder sb = new StringBuilder();
//                sb.append(String.format("IF( %s ,\n", actionConditionSb.toString()));
//                if (checkItem.仕様説明.contains("<チェック条件>")) {
//                    String checkCondtion = checkItem.仕様説明.substring(checkItem.仕様説明.indexOf("<チェック条件>") + "<チェック条件>".length());
//                    checkCondtion = checkCondtion.replace("\n", " ");
//                    sb.append(String.format("      IF((%s),\n", checkCondtion));
//                } else if (checkItem.仕様説明.contains("(＜チェック条件＞")) {
//                    String checkCondtion = checkItem.仕様説明.substring(checkItem.仕様説明.indexOf("(＜チェック条件＞") + "(＜チェック条件＞".length());
//                    checkCondtion = checkCondtion.replace("\n", " ");
//                    sb.append(String.format("      IF((%s),\n", checkCondtion));
//                } else {
//                    sb.append(String.format("      IF((%s),\n", "XXXXXXXX"));
//                }
//                sb.append(String.format("          @FALSE,\n"));
//                sb.append(String.format("          @TRUE),\n"));
//                sb.append(String.format("@TRUE)"));
//                ioItem.condition = escapseXml(sb.toString());
                ioItem.is_disable = "true";
                ioItemList.add(ioItem);
            }
        }
        replaceMap.put("ioItemList", ioItemList);
        return replaceMap;

    }

    private void setBiko(ScreenItemDescription itemBean, IOItem ioItem, Map<String, List<IOItem>> itemNameMap) {
        String bikoText = itemBean.getRemarks(); // itemBean.備考;
        if (!hasValue(bikoText)) {
            return;
        }
        Function<String, String> nameConverter = (s) -> {
            return convertGmName2Id(itemNameMap, s);
        };
        List<ItemPropMapping> ioItemPropDefine = Arrays.asList(//
                new ItemPropMapping("ラベル付加", "labelAvailable"), //
                new ItemPropMapping("ラベル式", "labelStatement", nameConverter), //
                new ItemPropMapping("ラベル文字", "labelText", nameConverter), //
                new ItemPropMapping("タイプ", "fieldType"), //
                new ItemPropMapping("アクション履歴なし", "noHistory"), //
                new ItemPropMapping("スタイル", "fieldStyle"), //
                new ItemPropMapping("行番号表示", "lineNumber"));
        String[] bikoList = bikoText.split("\n");
        for (String biko : bikoList) {
            boolean matched = false;
            for (ItemPropMapping mapping : ioItemPropDefine) {
                if (biko.startsWith(mapping.propLabel)) {
                    biko = biko.replace("：", ":");
                    if (biko.indexOf(":") != -1) {
                        String value = biko.substring(biko.indexOf(":") + 1);
                        if (ioItem.io_item_prop_list == null) {
                            ioItem.io_item_prop_list = new ArrayList<ItemProp>();
                        }
                        ioItem.io_item_prop_list.add(new ItemProp(mapping.propKey, escapseXml(mapping.apply(value))));
                        matched = true;
                    }
                    continue;
                }
            }
            if (!matched) {
                writeWarnLog("項番[{}] unkonw 備考:{}", itemBean.getItemNo(), biko);
            }
        }
        ioItem.description = escapseXml(bikoText);
    }

    private static final Pattern CONDTION_PATTERN = Pattern.compile("([^　 =]+) *=");

    private String getCondition(ScreenItemDescription itemBean, Map<String, List<IOItem>> itemNameMap) {
        // 表示条件
        String condition = itemBean.getDisplayCondition();
        if (!hasValue(condition)) {
            return null;
        }
        condition = convertGmName2Id(itemNameMap, condition);
        return escapseXml(condition);
    }

    private String getInitValue(ScreenItemDescription itemBean) {
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

    private String getStatement(ScreenItemDescription itemBean) {
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
                List<String> valueList = map.get("項目");

                if (dmList == null || dmList.isEmpty()) {
                    writeWarnLog("項番[{}] 加工式の【DM】が見つかりません", itemBean.getItemNo());
                    return "'" + escapseXml(statement) + "'";
                }
                String dm = dmList.get(0);

                TableBean findedTableItem = context.getTableSearchService().findTableByFullName(dm);
                if (findedTableItem == null) {
                    writeErrorLog("項番[{}] 加工式の【DM】が見つかりません:{}", itemBean.getItemNo(), dm);
                    return "'" + escapseXml(statement) + "'";
                }
                String dmCodeResult = findedTableItem.getTableName();
                String conditionResult = "";
                if (conditionList == null || conditionList.isEmpty()) {
                    writeErrorLog("項番[{}] 加工式の【条件】が見つかりません", itemBean.getItemNo());
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
                            writeErrorLog("項番[{}] 加工式の【条件】が見つかりません:{}", itemBean.getItemNo(), filedFullName);
                            continue;
                        }
                        m.appendReplacement(sb, fb.getFieldName() + "=");
                    }
                    m.appendTail(sb);
                    conditionResult = sb.toString();
                }
                String valueResult = "";
                if (valueList == null || valueList.isEmpty()) {
                    writeErrorLog("項番[{}] 加工式の【値】が見つかりません", itemBean.getItemNo());
                } else {
                    String value = valueList.get(0);
                    FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), value);
                    if (fb == null) {
                        writeErrorLog("項番[{}] 加工式の【項目】が見つかりません:{}", itemBean.getItemNo(), value);
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

    private ChoiceBean getChoiceInfo(ScreenItemDescription itemBean) {
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
        List<String> orderList = map.get("ソート順");
        if (dmList == null || dmList.isEmpty()) {
            writeWarnLog("項番[{}] 選択リストの【DM】が見つかりません", itemBean.getItemNo());
            return choiceInfo;
        }
        String dm = dmList.get(0);

        TableBean findedTableItem = context.getTableSearchService().findTableByFullName(dm);
        if (findedTableItem == null) {
            writeErrorLog("項番[{}] 選択リストの【DM】が見つかりません:{}", itemBean.getItemNo(), dm);
            return choiceInfo;
        }
        if (choiceInfo == null) {
            choiceInfo = new ChoiceBean();
        }
        choiceInfo.dmCode = findedTableItem.getTableName();
        if (conditionList == null || conditionList.isEmpty()) {
            writeErrorLog("項番[{}] 選択リストの【条件】が見つかりません", itemBean.getItemNo());
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
                    writeErrorLog("項番[{}] 選択リストの【条件】が見つかりません:{}", itemBean.getItemNo(), filedFullName);
                    continue;
                }
                m.appendReplacement(sb, fb.getFieldName() + "=");
            }
            m.appendTail(sb);
            choiceInfo.condition = sb.toString();
        }
        if (valueList == null || valueList.isEmpty()) {
            writeErrorLog("項番[{}] 選択リストの【値】が見つかりません", itemBean.getItemNo());
        } else {
            String value = valueList.get(0);
            FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), value);
            if (fb == null) {
                writeErrorLog("項番[{}] 選択リストの【値】が見つかりません:{}", itemBean.getItemNo(), value);
            } else {
                choiceInfo.valueDmItemCode = fb.getFieldName();
            }
        }
        if (labelList == null || labelList.isEmpty()) {
            writeErrorLog("項番[{}] 選択リストの【名称】が見つかりません", itemBean.getItemNo());
        } else {
            for (int i = 0; i < labelList.size(); i++) {
                String label = labelList.get(i);
                FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), label);
                if (fb == null) {
                    writeErrorLog("項番[{}] 選択リストの【名称】が見つかりません:{}", itemBean.getItemNo(), label);
                } else {
                    try {
                        BeanUtils.setProperty(choiceInfo, "nameDmItemCode" + (i + 1), fb.getFieldName());
                    } catch (Exception e) {
                        writeErrorLog("項番[{}] 選択リスの【名称】設定エラー", itemBean.getItemNo(), label, e);
                    }
                }
            }
        }

        if (orderList == null || orderList.isEmpty()) {
            writeErrorLog("項番[{}] 選択リストの【ソート順】が見つかりません", itemBean.getItemNo());
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
                    writeErrorLog("項番[{}] 選択リストの【表示順】が見つかりません:{}", itemBean.getItemNo(), order);
                } else {
                    try {
                        BeanUtils.setProperty(choiceInfo, "sortDmItemCode" + (i + 1), fb.getFieldName());
                    } catch (Exception e) {
                        writeErrorLog("項番[{}] 選択リストの【表示順】設定エラー", itemBean.getItemNo(), order, e);
                    }
                    try {
                        BeanUtils.setProperty(choiceInfo, "sortType" + (i + 1), orderType);
                    } catch (Exception e) {
                        writeErrorLog("項番[{}] 選択リストの【表示順】設定エラー", itemBean.getItemNo(), order, e);
                    }
                }
            }
        }
        return choiceInfo;
    }

    /**
     * 加工式解析
     * 
     * @param itemBean
     * @param defineValue
     * @return
     */
    private Map<String, List<String>> parseDefineCell(ScreenItemDescription itemBean, String defineValue) {
        Map<String, List<String>> map = new LinkedHashMap<String, List<String>>();

        String[] lines = defineValue.trim().split("\n");
        List<String> curList = null;
        for (String line : lines) {
            if (line.startsWith("[")) {
                line = line.substring(1);
                if (line.indexOf("]") == -1) {
                    writeErrorLog("項番[{}] 選択リストの解析失敗、「]」が見つかりません:{}", itemBean.getItemNo(), defineValue);
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
