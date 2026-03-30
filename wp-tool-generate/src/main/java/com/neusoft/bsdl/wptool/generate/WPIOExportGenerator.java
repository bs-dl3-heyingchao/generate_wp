package com.neusoft.bsdl.wptool.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.neusoft.bsdl.wptool.core.model.CsvLayout;
import com.neusoft.bsdl.wptool.core.model.CsvSubLayout;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.service.IWPTableSearchService;
import com.neusoft.bsdl.wptool.core.service.impl.WPTableSearchUtils;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;
import com.neusoft.bsdl.wptool.generate.model.IOItem;
import com.neusoft.bsdl.wptool.generate.utils.GenerateUtils;

import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WPIOExportGenerator extends WPAbstractGenerator<ExcelSheetContent<CsvLayout>> {
    private IWPTableSearchService combinedTableSearchService;

    public WPIOExportGenerator(WPGenerateContext context, List<ExcelSheetContent<CsvLayout>> excelContents) {
        this(context, excelContents, null);
    }

    public WPIOExportGenerator(WPGenerateContext context, List<ExcelSheetContent<CsvLayout>> excelContent, List<DBQuerySheetContent> dbQuerySheetContents) {
        super(context, excelContent);
        loadDBQuerySheetContents(dbQuerySheetContents);
    }

    private void loadDBQuerySheetContents(List<DBQuerySheetContent> dbQuerySheetContents) {
        IWPTableSearchService dbQuerySearchService = WPTableSearchUtils.createDBQueryTableSearchService(dbQuerySheetContents);
        this.combinedTableSearchService = WPTableSearchUtils.createCombinedTableSearchService(dbQuerySearchService, context.getTableSearchService());
    }

    @Override
    public String[] getTemplateNames() {
        return new String[] { "io" };
    }

    @Override
    public Map<String, Object> getReplaceMap(ExcelSheetContent<CsvLayout> excelContent) {
        CsvLayout csvLayout = excelContent.getContent();
        this.logPrefix = String.format("[%s:%s %s]", csvLayout.getFileId(), csvLayout.getFileName(), excelContent.getSheetName());

        Map<String, Object> replaceMap = new HashMap<String, Object>();
        Set<String> codeSet = new HashSet<>();
        List<IOItem> ioItemList = new ArrayList<>();

        replaceMap.put("io_type", "EXPORT");
        replaceMap.put("gmId", csvLayout.getFileId());
        replaceMap.put("gmIoId", csvLayout.getFileId());
        replaceMap.put("gmName", csvLayout.getFileName());

        for (CsvSubLayout itemBean : csvLayout.getCsvSubLayouts()) {
            String codePrefix = "O_";
            String baseCode = "";

            this.logSubPrefix = String.format("項番[%s]", itemBean.getItemNo());
            IOItem ioItem = new IOItem();
            ioItem.io_code = csvLayout.getFileId();
            ioItem.name = escapseXml(itemBean.getItemName());

            boolean hasModelInfo = false;

            
            if (hasValue(itemBean.getTableName())) {
                hasModelInfo = true;
                // 対象テーブル情報
                String tableFullName = itemBean.getTableName().replaceAll("[\r\n]", "");
                String fieldFullName = itemBean.getFiledName().replaceAll("[\r\n]", "");
                TableBean tb = this.combinedTableSearchService.findTableByFullName(tableFullName);
                if (tb != null) {
                    if ("DM".equals(itemBean.getAttributeWP())) {
                        ioItem.dm_code = tb.getTableName();
                    }
                    FieldBean fb = this.combinedTableSearchService.findFieldByFullName(tableFullName, fieldFullName);
                    if (fb != null) {
                        baseCode = fb.getFieldName();

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
                    if (StringUtils.isEmpty(baseCode)) {
                        String id = context.getMorphemHelper().getRomaFromKanji(itemBean.getItemName()).toUpperCase();
                        baseCode = id;
                    }
                }
            } else {
                if(hasValue(itemBean.getConstValue())) {
                    ioItem.statement = escapseXml(itemBean.getConstValue());
                } 
            }
            if (StringUtils.isNotEmpty(codePrefix)) {
                // データモデルに紐づく項目，表示是不要前缀
                if (hasModelInfo && codePrefix.startsWith("O_")) {
                    codePrefix = codePrefix.replaceFirst("O_", "");
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

            if (itemBean.getDisplay() != null && itemBean.getDisplay().contains("非表示")) {
                ioItem.is_visible = "false";
            } else {
                ioItem.is_visible = "true";
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
                        writeErrorLog("ソート順の形式が不正なソートタイプです:{}", sortType);
                    }
                } else {
                    writeErrorLog("ソート順の形式が不正です:{}", itemBean.getSorted());
                }
            }
            ioItem.description = escapseXml(itemBean.getRemarks());
            ioItem.code = GenerateUtils.addAndGetUniqueCode(codePrefix + baseCode, codeSet);
            ioItemList.add(ioItem);
        }
        replaceMap.put("ioItemList", ioItemList);
        return replaceMap;
    }
}
