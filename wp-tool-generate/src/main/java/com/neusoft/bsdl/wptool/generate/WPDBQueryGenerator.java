package com.neusoft.bsdl.wptool.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.neusoft.bsdl.wptool.core.model.DBQueryEntity;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;
import com.neusoft.bsdl.wptool.generate.model.DmItem;
import com.neusoft.bsdl.wptool.generate.model.DmProp;

public class WPDBQueryGenerator extends WPAbstractGenerator<DBQuerySheetContent> {

    public WPDBQueryGenerator(WPGenerateContext context, DBQuerySheetContent excelContent) {
        super(context, excelContent);
    }

    @Override
    public String[] getTemplateNames() {
        return new String[] { "dq" };
    }

    @Override
    public Map<String, Object> getReplaceMap(DBQuerySheetContent excelContent) {
        Map<String, Object> replaceMap = new HashMap<String, Object>();
        replaceMap.put("dmId", excelContent.getTableId());
        replaceMap.put("dmName", escapseXml(excelContent.getTableName()));
        List<DmItem> dmList = new ArrayList<DmItem>();
        for (DBQueryEntity fb : excelContent.getQueryEntities()) {
            DmItem dmItem = new DmItem();
            dmItem.data_type = fb.getDataTypeWP();
            dmItem.code = fb.getPhysicalName();
            dmItem.name = escapseXml(fb.getLogicalName());
            dmItem.length = fb.getLengthPre();
            dmItem.byteSize = fb.getLengthB();
            dmItem.scale = fb.getLengthS();
            dmItem.is_nullable = fb.getIsNullable() ? "true" : "false";
            dmItem.key_group = fb.getKeyGroup();
            dmList.add(dmItem);
        }
        replaceMap.put("dmItemList", dmList);
        
        List<DmProp> dmPropList = new ArrayList<DmProp>();
//        dmPropList.add(new DmProp("dbQuery", escapseXml("select xxxx from xxx\n where 1=1"), "false"));
//        dmPropList.add(new DmProp("dbQueryAggregate", escapseXml("select xxxx\n from xxx \n where 2=2"), "false"));
        replaceMap.put("dmPropList", dmPropList);
        return replaceMap;
    }

}
