package cbai.web.app.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.sqlconvert.SqlConverterAbstract;
import cbai.util.sqlconvert.SqlConverterAbstract.FindedTableItem;
import cbai.web.app.toolbox.services.SqlConvertService;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("dmLogic")
@Slf4j
public class DmLogicController {
    @Autowired
    private SqlConvertService sqlConvertService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("tableNamePlace", "例：見積備考");
        StringBuilder sb = new StringBuilder();
        sb.append("例：\n");
        sb.append("見積№\n");
        sb.append("見積枝番\n");
        sb.append("\n");
        model.addAttribute("inputPlace", sb.toString());
        return "dmLogic/index";
    }

    @PostMapping(value = { "/convert" })
    @ResponseBody
    public String convert(@RequestParam String srcValue, @RequestParam String tableName,
            @RequestParam(value = "projectId", required = false, defaultValue = "${toolbox.default-project-id}") String projectId) {
        String output = "";
        tableName = tableName.trim();
        if (StringUtils.isEmpty(tableName)) {
            output = "论理名不能为空";
            log.error("output:" + output);
            return output;
        }
        try {
            log.debug("input:" + srcValue);
            SqlConverterAbstract sqlConverter = this.sqlConvertService.loadSqlConverter(projectId);
            FindedTableItem tableItem = sqlConverter.findTableItem(tableName, null);
            if (!tableItem.isFind()) {
                output = "没有找到论理名为" + tableName + "的表";
                log.error("output:" + output);
                return output;
            }
            log.debug("input:" + srcValue);
            Map<String, FieldBean> fieldMap = tableItem.getTableBean().getFieldMap();
            String[] lines = srcValue.replaceAll("\r", "").split("\n");
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                if (StringUtils.isEmpty(line)) {
                    sb.append("\n");
                    continue;
                }
                String[] cols = line.trim().split("\t+");
                String fieldName = "";
                String valueForSet = "";
                if (cols.length > 1) {
                    valueForSet = cols[1];
                }
                String fieldFullName = cols[0];
                FieldBean fb = fieldMap.get(fieldFullName);
                if (fb == null && fieldFullName.contains("ID")) {
                    String tmp = fieldFullName.replace("ID", "ＩＤ");
                    fb = fieldMap.get(tmp);
                } else if (fb == null && fieldFullName.contains("ＩＤ")) {
                    String tmp = fieldFullName.replace("ＩＤ", "ID");
                    fb = fieldMap.get(tmp);
                }

                if (fb != null) {
                    fieldName = fb.getFieldName();
                } else {
                    fieldName = fieldFullName;
                }
                sb.append(fieldName).append("\t");
                String comment = fieldFullName;
                sb.append(getFieldValue(fieldName, valueForSet)).append("\t\t\t\t").append(comment).append("\n");
            }
            output = sb.toString();
        } catch (Exception e) {
            log.error("sqlConvert", e);
            output = e.getMessage();
        }
        log.debug("output:" + output);
        return output;

    }

    private String getFieldValue(String fieldName, String valueForSet) {
        String fieldValue = "_IN_._ITEM_";
        switch (fieldName) {
        case "INS_USR_ID":
        case "UPD_USR_ID":
            fieldValue = "CODE(@USER)";
            break;
        case "INS_PRG_ID":
        case "UPD_PRG_ID":
            fieldValue = "CODE(@IOCODE)";
            break;
        case "INS_DT":
        case "UPD_DT":
            fieldValue = "@SYSNOW";
            break;
        default:
            break;
        }
        return fieldValue;
    }
}
