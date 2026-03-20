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
import com.neusoft.bsdl.wptool.core.model.ProcessingFuncSpecification;
import com.neusoft.bsdl.wptool.core.model.ProcessingFuncSpecificationParam;
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

    private static final Map<String, String> CHECK_KBN_MAP = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put("必須チェック", "REQ");
            put("存在チェック", "EXT");
            put("重複チェック", "DUP");
            put("範囲チェック", "RNG");
            put("逆転チェック", "RNG");
            put("最大値チェック", "RNG_MAX");
            put("最小値チェック", "RNG_MIN");
            put("フォーマットチェック", "FMT");
            put("文字種チェック", "FMT");
            put("可能チェック", "BIZ_ABLE");
            put("妥当性チェック", "BIZ_VALID");
            put("変更チェック", "BIZ_CHG");
            put("チェック", "BIZ"); // 默认
        }
    };

    public enum IOType {
        IO, EXPORT
    }

    /**
     * IOタイプ（IO/EXPORT）
     */
    private IOType ioType = IOType.IO;

    private Map<String, String> paramNameToCodeMap;
    private Map<String, IOItem> itemIdMap;
//    private Map<String, List<IOItem>> itemNameMap;
    private Set<String> codeSet;

    public WPIOGenerator(WPGenerateContext context, ScreenExcelContent excelContent) {
        super(context, excelContent);
    }

    public WPIOGenerator(WPGenerateContext context, ScreenExcelContent excelContent, IOType ioType) {
        super(context, excelContent);
        this.ioType = ioType == null ? IOType.IO : ioType;
    }

    @Override
    public String[] getTemplateNames() {
        return new String[] { "io" };
    }

    private static final Pattern[] GM_ITEM_PATTERN_LIST = new Pattern[] { Pattern.compile("[\\S&&[^.]]+\\.[\\S&&[^.]]+"), Pattern.compile("[\\S&&[^-.,=><]]+\\.[\\S&&[^-.,=<>]]+"),
            Pattern.compile("[\\S&&[^-(.,=><]]+\\.[\\S&&[^-).,=<>]]+") };

    private String normalizeCondition(String condition) {
        condition = condition.replace("＝", "=");
        condition = condition.replace("かつ", " and ");
        condition = condition.replace("　", " ");
        condition = condition.replaceAll("\\. *", "\\.");
        condition = condition.replace("’", "'");
        condition = condition.replace("’", "'");
        condition = condition.replace("\n", " ");
        return condition;
    }

    private IOItem findItemCodeByName(String itemName) {
        for (IOItem item : itemIdMap.values()) {
            if (item.name.equals(itemName)) {
                return item;
            }
        }
        return null;
    }

    private String convertGmName2Id(String content) {
        content = content.replaceAll("\t", " ");
        content = content.replaceAll("－", "-");
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
                // 画面项目
                if ("画面".equals(temp[0])) {
                    if (findItemCodeByName(temp[1]) != null) {
                        IOItem tmpIoItem = findItemCodeByName(temp[1]);
                        String code = tmpIoItem.code;
                        if (code != null) {
                            matcher.appendReplacement(sb, code);
                        }
                    }
                } else if ("セッション".equals(temp[0])) {
                    String sessionKey = context.getSessionItemLoaderService().findSessionKeyByName(temp[1]);
                    if (sessionKey != null) {
                        matcher.appendReplacement(sb, sessionKey);
                    } else {
                        writeErrorLog("セッション名 '{}' がセッションアイテム定義に見つかりません。GM表記 '{}' をコードに変換できません。", temp[1], item);
                    }
                } else if ("パラメータ".equals(temp[0])) {
                    if (paramNameToCodeMap != null && paramNameToCodeMap.containsKey(temp[1])) {
                        String code = paramNameToCodeMap.get(temp[1]);
                        if (code != null) {
                            matcher.appendReplacement(sb, "@" + code);
                        }
                    } else {
                        writeErrorLog("パラメータ名 '{}' が処理機能記述書のパラメータに見つかりません。GM表記 '{}' をコードに変換できません。", temp[1], item);
                    }
                }
            }
            matcher.appendTail(sb);
            content = sb.toString();
        }
        if (content.contains("画面.") || content.contains("セッション.") || content.contains("パラメータ.")) {
            writeWarnLog("条件 '{}' に画面表記が残っています。コードに変換できない可能性があります。", content);
        }
        return content;
    }

    private String addAndGetUniqueCode(String baseCode, Set<String> codeSet) {
        while (!codeSet.add(baseCode)) {
            writeWarnLog("コード '{}' は既に存在しています。ユニークなコードを生成します。", baseCode);
            if (baseCode.matches(".*_\\d+$")) {
                try {
                    String part1 = baseCode.substring(0, baseCode.lastIndexOf("_"));
                    String part2 = baseCode.substring(baseCode.lastIndexOf("_") + 1);
                    int suffixLen = part2.length();
                    int index = Integer.parseInt(part2);
                    suffixLen = Math.max(suffixLen, String.valueOf(index + 1).length());
                    baseCode = part1 + "_" + StringUtils.leftPad(String.valueOf((index + 1)), suffixLen, '0');
                } catch (Exception e) {
                    baseCode = baseCode + "_01";
                }
            } else {
                baseCode = baseCode + "_01";
            }
            baseCode = baseCode.replaceAll("_+", "_");
        }
        return baseCode;
    }

    @Override
    public Map<String, Object> getReplaceMap(ScreenExcelContent screenExcelContent) {
        Map<String, Object> replaceMap = new HashMap<String, Object>();
        // 画面定義書
        processScreenExcelScreenDefinition(screenExcelContent, replaceMap);
        // 処理機能記述書
        processScreenExcelSpecification(screenExcelContent);

//        this.itemNameMap = new HashMap<String, List<IOItem>>();
        this.codeSet = new HashSet<String>();
        this.itemIdMap = new LinkedHashMap<String, IOItem>();

        List<IOItem> ioItemList = new ArrayList<>();
        processExcelSheetScreenItemCreateItemIdMap(screenExcelContent, itemIdMap);
        processExcelSheetScreenItem(screenExcelContent, replaceMap, ioItemList);
        // 画面チェック仕様書
        processExcelSheetScreenValidation(screenExcelContent, ioItemList);
        replaceMap.put("ioItemList", ioItemList);
        return replaceMap;

    }

    private void processExcelSheetScreenValidation(ScreenExcelContent screenExcelContent, List<IOItem> ioItemList) {
        ExcelSheetContent<List<ScreenValidation>> excelSheetScreenValidation = findSheetContentList(screenExcelContent, "画面チェック仕様書", ScreenValidation.class);
        if (excelSheetScreenValidation.getContent() != null && !excelSheetScreenValidation.getContent().isEmpty()) {
            this.logPrefix = String.format("[%s:%s %s]", screenExcelContent.getScreenId(), screenExcelContent.getScreenName(), excelSheetScreenValidation.getSheetName());

//            Map<String, Integer> actionIndexMap = new HashMap<String, Integer>();
            for (ScreenValidation checkItem : excelSheetScreenValidation.getContent()) {
                String baseCode = "";
                if ("BP".equalsIgnoreCase(checkItem.getBizWarining())) {
                    // TODO BP Check
                    continue;
                } else if ("ワーニング".equalsIgnoreCase(checkItem.getBizWarining())) {
                    // TODO ワーニング Check ?
                    continue;
                }
                this.logSubPrefix = String.format("項番[%s]", checkItem.getItemNo());
                IOItem ioItem = new IOItem();
                ioItem.io_code = screenExcelContent.getScreenId();
//                ioItem.name = escapseXml(checkItem.チェックアクション.trim().replace("\n", "／") + " " + checkItem.チェック名);
                ioItem.name = escapseXml(checkItem.getValidationName());
                ioItem.is_visible = "false";
                ioItem.item_type = "C";
                String codePrefix = "C_";
                String checkKbnCode = getCheckKbnCode(checkItem.getValidationName());
                if (StringUtils.isNotEmpty(checkKbnCode)) {
                    codePrefix = codePrefix + checkKbnCode + "_";
                }
                List<ScreenValidationAction> actions = checkItem.getValidationActions();
                StringBuilder actionConditionSb = new StringBuilder();

                // 从画面查的指定项目的ID，如果找不到就用罗马字转换的ID
                IOItem tmpIoItem = findItemCodeByName(checkItem.getItemName());
                if (tmpIoItem != null) {
                    baseCode = tmpIoItem.code;
                } else {
                    String id = context.getMorphemHelper().getRomaFromKanji(checkItem.getItemName()).toUpperCase();
                    baseCode = id;
                }
                if (actions.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (ScreenValidationAction action : actions) {
                        if (!action.isHasChecked()) {
                            continue;
                        }
                        tmpIoItem = findItemCodeByName(action.getActionName());
                        String name = "";
                        if (tmpIoItem != null) {
                            if (tmpIoItem.code.startsWith("A_")) {
                                name = tmpIoItem.code.substring(2);
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
                }
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
                            param = convertGmName2Id(param);
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
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("IF( %s ,\n", actionConditionSb.toString()));
                if (checkItem.getValidationRule().contains("<チェック条件>")) {
                    String checkCondtion = checkItem.getValidationRule().substring(checkItem.getValidationRule().indexOf("<チェック条件>") + "<チェック条件>".length());
                    checkCondtion = checkCondtion.replace("\n", " ");
                    sb.append(String.format("      IF((%s),\n", checkCondtion));
                } else if (checkItem.getValidationRule().contains("(＜チェック条件＞")) {
                    String checkCondtion = checkItem.getValidationRule().substring(checkItem.getValidationRule().indexOf("(＜チェック条件＞") + "(＜チェック条件＞".length());
                    checkCondtion = checkCondtion.replace("\n", " ");
                    sb.append(String.format("      IF((%s),\n", checkCondtion));
                } else {
                    sb.append(String.format("      IF((%s),\n", "TODO XXXXXXXX"));
                }
                sb.append(String.format("          @FALSE,\n"));
                sb.append(String.format("          @TRUE),\n"));
                sb.append(String.format("@TRUE)"));
                ioItem.condition = escapseXml(sb.toString());
                ioItem.is_disable = "true";
                ioItem.code = addAndGetUniqueCode(codePrefix + baseCode, codeSet);
                ioItemList.add(ioItem);
            }
        }
    }

    private String createCodeKey(String screenId, String sheetName, String itemNo) {
        return screenId + "_" + sheetName + "_" + itemNo;
    }

    /**
     * 画面项目进行两次处理，第一次只生成各项目的ID
     * 
     * @param screenExcelContent
     * @param itemIdMap
     */
    private void processExcelSheetScreenItemCreateItemIdMap(ScreenExcelContent screenExcelContent, Map<String, IOItem> itemIdMap) {
        int groupIndex = 0;
        boolean isInGroup = false;
        String curGroupPrefix = "";
        ExcelSheetContent<List<ScreenItemDescriptionResult>> excelSheetScreenItem = findSheetContentList(screenExcelContent, "画面項目説明書", ScreenItemDescriptionResult.class);
        if (excelSheetScreenItem == null) {
            throw new WPException("画面項目説明書シートが見つかりません");
        }
        List<ScreenItemDescriptionResult> list = null;
        switch (ioType) {
        case IO:
            list = excelSheetScreenItem.getContent();
            break;
        case EXPORT:
            throw new WPException("エクスポートIOの生成はまだ実装されていません");
        default:
            throw new WPException("不明なIOタイプ: " + ioType);
        }
        this.logPrefix = String.format("[%s:%s %s]", screenExcelContent.getScreenId(), screenExcelContent.getScreenName(), excelSheetScreenItem.getSheetName());

        for (ScreenItemDescriptionResult itemGroup : list) {
            curGroupPrefix = "";
            isInGroup = false;
            for (ScreenItemDescription itemBean : itemGroup.getItems()) {
                String display = itemBean.getDisplay();// itemBean.表示;
                String itemType = "";
                String io = itemBean.getIo();// itemBean.IO;
                String codePrefix = "X_";
                String baseCode = "";
                this.logSubPrefix = String.format("項番[%s]", itemBean.getItemNo());
                if (io.contains("I入力")) {
                    itemType = "I";
                    codePrefix = "I_";
                } else if (io.contains("IO入出力")) {
                    itemType = "IO";
                    codePrefix = "I_";
                } else if (io.contains("O出力")) {
                    itemType = "O";
                    codePrefix = "O_";
                } else if (io.contains("Aアクション")) {
                    itemType = "A";
                    codePrefix = "A_";
                } else if (io.contains("Gグループ")) {
                    itemType = "G";
                    groupIndex++;
                    isInGroup = true;
                    codePrefix = "";
                    curGroupPrefix = "G" + groupIndex + "_";
                    baseCode = "G" + groupIndex;
                }

                if (display != null && display.contains("非表示")) {
                    codePrefix = "H_";
                }
                // 在Group中的项目
                if (isInGroup && !"G".equals(itemType)) {
                    codePrefix = codePrefix + curGroupPrefix;
                }

                boolean hasModelInfo = false;
                if (hasValue(itemBean.getModelName()) /* && !tableFullName.endsWith("クエリ") */) {
                    hasModelInfo = true;
                    // 対象テーブル情報
                    String tableFullName = itemBean.getModelName().replaceAll("[\r\n]", "");
                    String fieldFullName = itemBean.getModelItemName().replaceAll("[\r\n]", "");
                    TableBean tb = context.getTableSearchService().findTableByFullName(tableFullName);
                    if (tb != null) {
                        FieldBean fb = context.getTableSearchService().findFieldByFullName(tableFullName, fieldFullName);
                        if (fb != null) {
                            baseCode = fb.getFieldName();
                        }
                    }
                } else {
                    if ("A".equals(itemType) && StringUtils.isEmpty(baseCode)) {
                        baseCode = getActionIoCode(itemBean.getItemName());
                    }
                    if (StringUtils.isEmpty(baseCode)) {
                        String id = context.getMorphemHelper().getRomaFromKanji(itemBean.getItemName()).toUpperCase();
                        baseCode = id;
                    }
                }
                if (!"G".equals(itemType) && StringUtils.isNotEmpty(codePrefix) /* && !"DM".equals(itemBean.getAttributeWP()) */) {
                    // データモデルに紐づく項目，表示是不要前缀
                    if (hasModelInfo && codePrefix.startsWith("O_")) {
                        codePrefix = codePrefix.replaceFirst("O_", "");
                    }
                }
                String itemCode = addAndGetUniqueCode(codePrefix + baseCode, codeSet);
                IOItem tmpIoItem = new IOItem();
                tmpIoItem.code = itemCode;
                tmpIoItem.name = itemBean.getItemName();
                itemIdMap.put(createCodeKey(screenExcelContent.getScreenId(), excelSheetScreenItem.getSheetName(), itemBean.getItemNo()), tmpIoItem);
            }
        }

    }

    private void processExcelSheetScreenItem(ScreenExcelContent screenExcelContent, Map<String, Object> replaceMap, List<IOItem> ioItemList) {
        String ioSuffix;
        boolean isInGroup = false;
        ExcelSheetContent<List<ScreenItemDescriptionResult>> excelSheetScreenItem = findSheetContentList(screenExcelContent, "画面項目説明書", ScreenItemDescriptionResult.class);
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
        this.logPrefix = String.format("[%s:%s %s]", screenExcelContent.getScreenId(), screenExcelContent.getScreenName(), excelSheetScreenItem.getSheetName());

        replaceMap.put("io_type", ioType.name());
        replaceMap.put("gmId", screenExcelContent.getScreenId());
        replaceMap.put("gmIoId", screenExcelContent.getScreenId() + ioSuffix);
        replaceMap.put("gmName", screenExcelContent.getScreenName());
//        replaceMap.put("parentDir", screenExcelContent.getScreenId());
        for (ScreenItemDescriptionResult itemGroup : list) {
            isInGroup = false;
            for (ScreenItemDescription itemBean : itemGroup.getItems()) {
                IOItem ioItem = new IOItem();
                IOItem tmpIoItem = itemIdMap.get(createCodeKey(screenExcelContent.getScreenId(), excelSheetScreenItem.getSheetName(), itemBean.getItemNo()));
                if (tmpIoItem == null) {
                    throw new WPException(String.format("項目IDが見つかりません。画面ID: %s, シート名: %s, 項番: %s", screenExcelContent.getScreenId(), excelSheetScreenItem.getSheetName(), itemBean.getItemNo()));
                }
                if (!StringUtils.equals(tmpIoItem.name, itemBean.getItemName())) {
                    throw new WPException(String.format("項目IDに対応する項目名が一致しません。画面ID: %s, シート名: %s, 項番: %s, 項目ID: %s, 項目名1: %s, 項目名2: %s", screenExcelContent.getScreenId(),
                            excelSheetScreenItem.getSheetName(), itemBean.getItemNo(), tmpIoItem.code, tmpIoItem.name, itemBean.getItemName()));
                }
                ioItem.code = tmpIoItem.code;
                ioItem.name = itemBean.getItemName();// itemBean.getItemName();
                ioItem.io_code = screenExcelContent.getScreenId();
                String display = itemBean.getDisplay();// itemBean.表示;
                String io = itemBean.getIo();// itemBean.IO;
                this.logSubPrefix = String.format("項番[%s]", itemBean.getItemNo());
                if (io.contains("I入力")) {
                    ioItem.item_type = "I";
                } else if (io.contains("IO入出力")) {
                    ioItem.item_type = "IO";
                } else if (io.contains("O出力")) {
                    ioItem.item_type = "O";
                } else if (io.contains("Aアクション")) {
                    ioItem.item_type = "A";
                } else if (io.contains("Gグループ")) {
                    ioItem.level = "1";
                    ioItem.item_type = "G";
                    isInGroup = true;
                } else {
                    writeErrorLog("unkonw I/O :{}", io);
                }
                if ("○".equalsIgnoreCase(itemBean.getRequired())) {
                    // 设计书中的必須不生成到WP的设定中，之后在 チェック仕様 中实现，这里全都设置成非必须
                    ioItem.is_require = "false";
                } else {
                    ioItem.is_require = "false";
                }

                if (display != null && display.contains("非表示")) {
                    ioItem.is_visible = "false";
                } else {
                    ioItem.is_visible = "true";
                }
                // 在Group中的项目
                if (isInGroup && !"G".equals(ioItem.item_type)) {
                    ioItem.level = "2";
                }
                if (hasValue(itemBean.getLengthWP())) {
                    if ("DM".equalsIgnoreCase(itemBean.getLengthWP())) {
                        // DM不设置【桁数】，使用默认值
                    } else if (itemBean.getLengthWP().matches("\\d+")) {
                        ioItem.length = itemBean.getLengthWP();
                    } else {
                        writeWarnLog("unkonw 桁数(WP) :{}", itemBean.getLengthWP());
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
                            writeWarnLog("ソート順の形式が不正なソートタイプです:{}", sortType);
                        }
                    } else {
                        writeWarnLog("ソート順の形式が不正です:{}", itemBean.getSorted());
                    }
                }

                if (hasValue(itemBean.getFormat())) {
                    ioItem.format = itemBean.getFormat();
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
                        } else {
                            ioItem.dm_item_code = fieldFullName;
                            writeErrorLog("テーブル項目が見つかりません:{}.{}", tableFullName, fieldFullName);
                        }
                    } else {
                        ioItem.dm_code = tableFullName;
                        writeErrorLog("テーブルが見つかりません:{}", tableFullName);
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
                // 初期値
                ioItem.default_value = getInitValue(itemBean);
                // 加工式
                ioItem.statement = getStatement(itemBean);
                // 選択リスト
                ioItem.choiceInfo = getChoiceInfo(itemBean);
                // 表示条件
                ioItem.condition = getCondition(itemBean);
                ioItemList.add(ioItem);
            }
        }
    }

    private void processScreenExcelSpecification(ScreenExcelContent screenExcelContent) {
        ExcelSheetContent<ProcessingFuncSpecification> screenExcelSpecification = findSheetContent(screenExcelContent, "処理機能記述書", ProcessingFuncSpecification.class);

        List<ProcessingFuncSpecificationParam> screenInputParams = null;
        if (screenExcelSpecification != null) {
            paramNameToCodeMap = new HashMap<String, String>();
            screenInputParams = screenExcelSpecification.getContent().getParams();
            screenInputParams.forEach((k) -> {
                paramNameToCodeMap.put(k.getLogicName(), k.getSort());
            });
        }
    }

    private void processScreenExcelScreenDefinition(ScreenExcelContent screenExcelContent, Map<String, Object> replaceMap) {
        ExcelSheetContent<ScreenDefinition> screenExcelScreenDefinition = findSheetContent(screenExcelContent, "画面定義書", ScreenDefinition.class);
        if (screenExcelScreenDefinition != null) {
            this.logPrefix = String.format("[%s:%s %s]", screenExcelContent.getScreenId(), screenExcelContent.getScreenName(), screenExcelScreenDefinition.getSheetName());
            ScreenDefinition dxtjBean = screenExcelScreenDefinition.getContent();
            if (dxtjBean.getTargetModels() != null && !dxtjBean.getTargetModels().isEmpty()) {
                String gmDmCode = dxtjBean.getTargetModels().get(0).getPhysicalName();
                replaceMap.put("gmDmCode", gmDmCode);
                // 対象条件
                String gmIoCondition = dxtjBean.getTargetCondition();
                if (StringUtils.isNotEmpty(gmIoCondition)) {
                    gmIoCondition = normalizeCondition(gmIoCondition);
                    String gmIoConditionConvered = context.getSqlConverter().convert(gmIoCondition);
                    replaceMap.put("gmIoCondition", escapseXml(gmIoConditionConvered));
                }
            }
        }
    }

    private String getCheckKbnCode(String validationName) {
        return CHECK_KBN_MAP.entrySet().stream().filter(e -> validationName.contains(e.getKey())).map(e -> e.getValue()).findFirst().orElse("");
    }

    private String getActionIoCode(String itemName) {
        // フッタ部のアクション （Fキー割当あり
        Pattern pattern = Pattern.compile("F(\\d+)");
        Matcher matcher = pattern.matcher(itemName);
        if (matcher.find()) {
            return "F" + StringUtils.leftPad(matcher.group(1), 2, "0");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <V> ExcelSheetContent<V> findSheetContent(ScreenExcelContent screenExcelContent, String sheetName, Class<V> clazz) {
        return (ExcelSheetContent<V>) screenExcelContent.getSheetList().stream().filter(s -> sheetName.equals(s.getSheetName())).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private <V> ExcelSheetContent<List<V>> findSheetContentList(ScreenExcelContent screenExcelContent, String sheetName, Class<V> clazz) {
        return (ExcelSheetContent<List<V>>) screenExcelContent.getSheetList().stream().filter(s -> sheetName.equals(s.getSheetName())).findFirst().orElse(null);
    }

    private void setBiko(ScreenItemDescription itemBean, IOItem ioItem) {
        String bikoText = itemBean.getRemarks(); // itemBean.備考;
        if (!hasValue(bikoText)) {
            return;
        }
        Function<String, String> nameConverter = (s) -> {
            return convertGmName2Id(s);
        };
        String[] bikoList = bikoText.split("\n");
        List<List<String>> propGroup = new ArrayList<>();
        for (String line : bikoList) {
            if (line.matches("^([^：:]+)[:：].*") || propGroup.isEmpty()) {
                propGroup.add(new ArrayList<String>());
            }
            propGroup.get(propGroup.size() - 1).add(line);
        }

        List<ItemPropMapping> ioItemPropDefine = Arrays.asList(//
                new ItemPropMapping("ラベル付加", "labelAvailable"), //
                new ItemPropMapping("ラベル式", "labelStatement", nameConverter), //
                new ItemPropMapping("ラベル文字", "labelText", nameConverter), //
                new ItemPropMapping("タイプ", "fieldType"), //
                new ItemPropMapping("アクション履歴なし", "noHistory"), //
                new ItemPropMapping("スタイル", "fieldStyle"), //
                new ItemPropMapping("行番号表示", "lineNumber"));

        for (List<String> groupItem : propGroup) {
            String biko = String.join("\n", groupItem);
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
                writeWarnLog("unkonw 備考:{}", biko);
            }
        }
        ioItem.description = escapseXml(bikoText);
    }

    private String getCondition(ScreenItemDescription itemBean) {
        // 表示条件
        String condition = itemBean.getDisplayCondition();
        if (!hasValue(condition)) {
            return null;
        }
        condition = convertGmName2Id(condition);
        return escapseXml(condition);
    }

    private String getInitValue(ScreenItemDescription itemBean) {
        String initValue = itemBean.getDefaultValue();
        if (!hasValue(initValue)) {
            return null;
        }
        if (initValue.startsWith("固定値：") || initValue.startsWith("固定値:")) {
            initValue = initValue.replaceAll("　", " ").trim();
            initValue = "'" + initValue.substring(4).trim() + "'";
        } else {
            initValue = convertGmName2Id(initValue);
        }
        return escapseXml(initValue);
    }

    private static final Pattern CONDTION_PATTERN = Pattern.compile("([^　 =]+) *=");
    private static final Pattern DOUBLE_QUOTE_PATTERN = Pattern.compile("\"([^\"]*)\"");

    private String convertDoubleQuoteInCondition(String condition) {
        Matcher m = DOUBLE_QUOTE_PATTERN.matcher(condition);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = m.group(1);
            m.appendReplacement(sb, "'" + value + "'");
            writeWarnLog("条件 '{}' 中のダブルクオーテーションで囲まれた値 '{}' をシングルクオーテーションに変換しました。", condition, value);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 加工式解析
     * 
     * @param itemBean
     * @param itemNameMap
     * @return
     */
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
                    writeWarnLog("加工式の【DM】が見つかりません:{}", statement);
                    return escapseXml(statement);
                }
                String dm = dmList.get(0);

                TableBean findedTableItem = context.getTableSearchService().findTableByFullName(dm);
                if (findedTableItem == null) {
                    writeErrorLog("加工式の【DM】が見つかりません:{}", dm);
                    return escapseXml(statement);
                }
                String dmCodeResult = findedTableItem.getTableName();
                String conditionResult = "";
                if (conditionList == null || conditionList.isEmpty()) {
                    writeErrorLog("加工式の【条件】が見つかりません:{}", statement);
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (String condition : conditionList) {
                        condition = normalizeCondition(condition);
                        sb.append(condition).append(" ");
                    }
                    Matcher m = CONDTION_PATTERN.matcher(sb.toString().trim());
                    sb.setLength(0);
                    while (m.find()) {
                        String filedFullName = m.group(1).trim();
                        FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), filedFullName);
                        if (fb == null) {
                            writeErrorLog("加工式の【条件】が見つかりません:{}", filedFullName);
                            continue;
                        }
                        m.appendReplacement(sb, fb.getFieldName() + "=");
                    }
                    m.appendTail(sb);
                    conditionResult = convertDoubleQuoteInCondition(sb.toString());
                }
                String valueResult = "";
                if (valueList == null || valueList.isEmpty()) {
                    writeErrorLog("加工式の【値】が見つかりません:{}", statement);
                } else {
                    String value = valueList.get(0);
                    FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), value);
                    if (fb == null) {
                        writeErrorLog("加工式の【項目】が見つかりません:{}", value);
                    } else {
                        valueResult = fb.getFieldName();
                    }
                }
                String newStatement = String.format("NAVAL(%s{%s}.%s, @NULL)", dmCodeResult, conditionResult, valueResult);
                return escapseXml(newStatement);
            } else if (map.containsKey("条件") && map.containsKey("設定値")) {
                List<String> conditionList = map.get("条件");
                if (conditionList == null || conditionList.isEmpty()) {
                    writeErrorLog("加工式の【条件】が見つかりません：{}", statement);
                    return escapseXml(statement);
                }
                List<String> setVals = map.get("設定値");
                if (setVals == null || setVals.isEmpty()) {
                    writeErrorLog("加工式の【設定値】が不正：{}", statement);
                    return escapseXml(statement);
                }
                String trueVal = null;
                String falseVal = null;
                for (String setVal : setVals) {
                    if (setVal.startsWith("true：")) {
                        trueVal = setVal.substring(5).trim();
                    } else if (setVal.startsWith("false：")) {
                        falseVal = setVal.substring(6).trim();
                    }
                }
                if (trueVal == null || falseVal == null) {
                    writeErrorLog("加工式の【設定値】が不正：{}", statement);
                    return escapseXml(statement);
                }
                Function<String, String> valConv = (s) -> {
                    // @NULL
                    if (s.startsWith("@")) {
                        return s;
                    }
                    if (s.startsWith("画面.") || s.startsWith("画面．") || s.startsWith("セッション.") || s.startsWith("セッション．") || s.startsWith("パラメータ.") || s.startsWith("パラメータ．")) {
                        return convertGmName2Id(s);
                    }
                    // 固定値
                    return "'" + s + "'";
                };
                StringBuilder sb = new StringBuilder();
                for (String condition : conditionList) {
                    condition = normalizeCondition(condition);
                    sb.append(condition).append(" ");
                }
                String newStatement = String.format("IF(%s, %s, %s)", convertGmName2Id(sb.toString().trim()), valConv.apply(trueVal), valConv.apply((falseVal)));
                return escapseXml(newStatement);
            }
        }
        if (statement.startsWith("@READONLY") || statement.startsWith("読み取り専用")) {
            return "@READONLY";
        }
        return escapseXml(statement);
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
            writeWarnLog("選択リストの【DM】が見つかりません", itemBean.getItemNo());
            return choiceInfo;
        }
        String dm = dmList.get(0);

        TableBean findedTableItem = context.getTableSearchService().findTableByFullName(dm);
        if (findedTableItem == null) {
            writeErrorLog("選択リストの【DM】が見つかりません:{}", itemBean.getItemNo(), dm);
            return choiceInfo;
        }
        if (choiceInfo == null) {
            choiceInfo = new ChoiceBean();
        }
        choiceInfo.dmCode = findedTableItem.getTableName();
        if (conditionList == null || conditionList.isEmpty()) {
            writeErrorLog("選択リストの【条件】が見つかりません", itemBean.getItemNo());
        } else {
            StringBuilder sb = new StringBuilder();
            for (String condition : conditionList) {
                condition = normalizeCondition(condition);
                sb.append(condition).append(" ");
            }
            Matcher m = CONDTION_PATTERN.matcher(sb.toString().trim());
            sb.setLength(0);
            while (m.find()) {
                String filedFullName = m.group(1).trim();
                FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), filedFullName);
                if (fb == null) {
                    writeErrorLog("選択リストの【条件】が見つかりません:{}", itemBean.getItemNo(), filedFullName);
                    continue;
                }
                m.appendReplacement(sb, fb.getFieldName() + "=");
            }
            m.appendTail(sb);
            choiceInfo.condition = convertDoubleQuoteInCondition(sb.toString());
        }
        if (valueList == null || valueList.isEmpty()) {
            writeErrorLog("選択リストの【値】が見つかりません", itemBean.getItemNo());
        } else {
            String value = valueList.get(0);
            FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), value);
            if (fb == null) {
                writeErrorLog("選択リストの【値】が見つかりません:{}", itemBean.getItemNo(), value);
            } else {
                choiceInfo.valueDmItemCode = fb.getFieldName();
            }
        }
        if (labelList == null || labelList.isEmpty()) {
            writeErrorLog("選択リストの【名称】が見つかりません", itemBean.getItemNo());
        } else {
            for (int i = 0; i < labelList.size(); i++) {
                String label = labelList.get(i);
                FieldBean fb = context.getTableSearchService().findFieldByFullName(findedTableItem.getTableFullName(), label);
                if (fb == null) {
                    writeErrorLog("選択リストの【名称】が見つかりません:{}", itemBean.getItemNo(), label);
                } else {
                    try {
                        BeanUtils.setProperty(choiceInfo, "nameDmItemCode" + (i + 1), fb.getFieldName());
                    } catch (Exception e) {
                        writeErrorLog("選択リスの【名称】設定エラー", itemBean.getItemNo(), label, e);
                    }
                }
            }
        }

        if (orderList == null || orderList.isEmpty()) {
            writeErrorLog("選択リストの【ソート順】が見つかりません", itemBean.getItemNo());
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
                    writeErrorLog("選択リストの【表示順】が見つかりません:{}", itemBean.getItemNo(), order);
                } else {
                    try {
                        BeanUtils.setProperty(choiceInfo, "sortDmItemCode" + (i + 1), fb.getFieldName());
                    } catch (Exception e) {
                        writeErrorLog("選択リストの【表示順】設定エラー", itemBean.getItemNo(), order, e);
                    }
                    try {
                        BeanUtils.setProperty(choiceInfo, "sortType" + (i + 1), orderType);
                    } catch (Exception e) {
                        writeErrorLog("選択リストの【表示順】設定エラー", itemBean.getItemNo(), order, e);
                    }
                }
            }
        }
        return choiceInfo;
    }

    /**
     * 加工式等定义格式解析 CASE-1<br>
     * 
     * <pre>
     * [DM]汎用マスタ
     * [条件]汎用データ区分＝"MT205"
     * [項目]汎用名
     * 
     * </pre>
     * 
     * CASE-2
     * 
     * <pre>
     * [条件]
     * 画面.契約締結年月＜ 画面.業務日付-6ヶ月(年月)
     * [設定値]
     * true：契約締結年月に6ヶ月以上前の過去日付が指定されています。
     * false：@NULL
     * 
     * </pre>
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
                    writeErrorLog("選択リストの解析失敗、「]」が見つかりません:{}", defineValue);
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
