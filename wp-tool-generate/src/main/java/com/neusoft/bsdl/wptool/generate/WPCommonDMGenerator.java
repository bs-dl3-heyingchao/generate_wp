package com.neusoft.bsdl.wptool.generate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.generate.model.DmItem;

import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.template.CreateTemplateVelocity;

public class WPCommonDMGenerator {
    private final static CreateTemplateVelocity createTemplate = new CreateTemplateVelocity("com.neusoft.bsdl.wptool.generate.WP_TEMPLATE", true);
    private WPContext context;

    public WPCommonDMGenerator(WPContext context) {
        this.context = context;
    }

    public void generate(File outputDir) throws IOException {
        List<TableBean> tableList = context.getTableSearchService().listAll();

        for (TableBean tb : tableList) {
            createTemplate.create(outputDir, getDmReplaceMap(tb), "dm");
        }
    }

    public Map<String, Object> getDmReplaceMap(TableBean tableBean) {
        Map<String, Object> replaceMap = new HashMap<>();
        replaceMap.put("dmId", tableBean.getTableName());
        replaceMap.put("dmName", tableBean.getTableFullName());
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
            dmItem.name = fb.getFieldFullName();
            dmItem.length = fb.getLen();
            dmItem.byteSize = "0";
            dmItem.scale = fb.getDotLen();
            dmItem.is_nullable = String.valueOf(!fb.isNotNull());
            dmItem.key_group = fb.isKey() ? "1" : "0";
            dmList.add(dmItem);
        }
        replaceMap.put("dmItemList", dmList);
        return replaceMap;
    }
}
