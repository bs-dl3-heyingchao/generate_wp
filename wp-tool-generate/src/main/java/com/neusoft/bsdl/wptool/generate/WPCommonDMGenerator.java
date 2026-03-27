package com.neusoft.bsdl.wptool.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;

import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;
import com.neusoft.bsdl.wptool.generate.model.DmItem;

import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;

public class WPCommonDMGenerator extends WPAbstractGenerator<TableBean> {

    public WPCommonDMGenerator(WPGenerateContext context, TableBean tableBean) {
        super(context, tableBean);
    }

    @Override
    public Map<String, Object> getReplaceMap(TableBean tableBean) {
        Map<String, Object> replaceMap = new HashMap<>();
        replaceMap.put("dmId", tableBean.getTableName());
        replaceMap.put("dmName", escapseXml(tableBean.getTableFullName()));
        List<DmItem> dmList = createDmItemList(tableBean);
        replaceMap.put("dmItemList", dmList);
        return replaceMap;
    }

    public static List<DmItem> createDmItemList(TableBean tableBean) {
        List<DmItem> dmList = new ArrayList<DmItem>();
        for (FieldBean fb : tableBean.getFieldList()) {
            DmItem dmItem = new DmItem();
            if (fb.getOthers() != null) {
                dmItem.data_type = fb.getOthers().get("WP_TYPE");
            }
            if (StringUtils.isEmpty(dmItem.data_type) || "FILE".equals(dmItem.data_type)) {
                if (StringUtils.isNotEmpty(fb.getType())) {
                    String type = fb.getType().toLowerCase();
                    if (type.contains("varchar")) {
                        dmItem.data_type = "TEXT";
                    } else if (type.contains("decimal")) {
                        dmItem.data_type = "NUM";
                    } else if (type.contains("image")) {
                        dmItem.data_type = "FILE";
                    }
                }
            }
            dmItem.code = fb.getFieldName();
            dmItem.name = StringEscapeUtils.escapeXml11(fb.getFieldFullName());
            if ("FILE".equals(dmItem.data_type) || "BOOL".equals(dmItem.data_type)) {
                dmItem.length = "0";
            } else {
                dmItem.length = fb.getLen();
            }
            dmItem.byteSize = "0";
            dmItem.scale = fb.getDotLen();
            dmItem.is_nullable = String.valueOf(!fb.isNotNull());
            dmItem.key_group = fb.isKey() ? "1" : "0";
            dmList.add(dmItem);
        }
        return dmList;
    }

    @Override
    public String[] getTemplateNames() {
        return new String[] { "dm" };
    }

}
