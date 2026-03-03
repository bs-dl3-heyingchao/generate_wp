package cbai.tool.sr;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cbai.util.FileUtil;
import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;

public class DmNameReplace {

	public static void main(String[] args) throws IOException {
		List<TableBean> tableList = SRUtils.getTableList();
		System.out.println(tableList.size());
		Map<String, String> tableItemMap = new HashMap<String, String>();
		Map<String, String> fieldItemMap = new HashMap<String, String>();
		for (TableBean tb : tableList) {
			if (StringUtils.isNotEmpty(tb.getTableFullName())) {
				tableItemMap.put(getNormalKey(tb.getTableName()), tb.getTableFullName());
			}
			for (FieldBean fb : tb.getFieldList()) {
				if (StringUtils.isNotEmpty(fb.getFieldFullName())) {
					fieldItemMap.put(getNormalKey(tb.getTableName()) + "_" + getNormalKey(fb.getFieldName()),
							fb.getFieldFullName());
					fieldItemMap.put(getNormalKey(fb.getFieldName()), fb.getFieldFullName());
				}
			}
		}

		System.out.println("-----------------------");
		Pattern p = Pattern.compile("<name>([^<]+)</name>");
		String dir = "C:\\Users\\bai.chen\\WP\\workspace\\SIRIUSRT\\dm";
		dir = dir.replace("\\", "/");
		List<String> list = FileUtil.listFileNames(dir, new String[] { ".wprx" });
		for (String file : list) {
			boolean isModify = false;
			String tableName = FileUtil.getFileName(file);
			tableName = tableName.substring(0, tableName.indexOf("."));
			if (!tableItemMap.containsKey(getNormalKey(tableName))) {
				System.err.println(tableName + " table is not find");
				continue;
			}
			String cnts = FileUtil.readString(file);
			if (cnts.contains("<name>" + tableName + "</name>")) {
				cnts = cnts.replaceFirst("<name>" + tableName + "</name>",
						"<name>" + tableItemMap.get(getNormalKey(tableName)) + "</name>");
				isModify = true;
			}
			StringBuffer sb = new StringBuffer();
			Matcher m = p.matcher(cnts);
			while (m.find()) {
				String fieldName = m.group(1);
				String key = getNormalKey(tableName) + "_" + getNormalKey(fieldName);
				if (fieldItemMap.containsKey(key)) {
					m.appendReplacement(sb, "<name>" + fieldItemMap.get(key) + "</name>");
					isModify = true;
				}
			}
			m.appendTail(sb);
			cnts = sb.toString();
			if (isModify) {
				System.out.println("modify :" + file);
				FileUtil.writeStringToFile(cnts, file, "UTF-8");
			}
		}

	}

	private static String getNormalKey(String key) {
		return key.toLowerCase();// .replace("_", "");
	}
}
