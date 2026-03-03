package cbai.tool.sr;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;

import cbai.util.FileUtil;
import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.log.UtilLogger;

public class SRDMVerCheck {
	private static final String wpPath = "D:\\MyDeveloper\\eclipse-workspace-sr\\sirius_rt-web-app-remote";
	private static final UtilLogger LOGGER = new UtilLogger();

	public static class ModifyTableInfo {
		public TableBean tb;
		public List<FieldBean> addedFieldList = new ArrayList<>();
		public List<FieldBean> deletedFieldList = new ArrayList<>();
		public List<FieldBean[]> modifyedFieldList = new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws SQLException, IOException {
		List<TableBean> oldList = loadTableListFromDM();
		List<TableBean> newList = (List<TableBean>) FileUtil.readObjectFromFile(new File("./db.cache"));
		Map<String, TableBean> newTableMap = new LinkedHashMap<String, TableBean>();
		for (TableBean t : newList) {
			List<FieldBean> newFieldList = new ArrayList<FieldBean>();
			List<FieldBean> fieldList = t.getFieldList();
			Set<String> keySet = new HashSet<>();
			for (FieldBean oldFb : fieldList) {
				String name = oldFb.getFieldName();
				if (name.endsWith("_FNM_") || name.endsWith("_FSZ_") || name.endsWith("_FTY_")
						|| name.endsWith("_FBD_")) {
					String newName = name.substring(0, name.length() - 5);
					if (keySet.add(newName)) {
						String fullName = oldFb.getFieldFullName();
						String newFullName = fullName.substring(0, fullName.indexOf("ファイル") + "ファイル".length());
						FieldBean fb = new FieldBean();
						fb.setFieldName(newName);
						fb.setFieldFullName(newFullName);
						newFieldList.add(fb);
					}
				} else {
					newFieldList.add(oldFb);
				}
			}
			t.setFieldList(newFieldList);
			newTableMap.put(t.getTableName(), t);
		}
		Map<String, TableBean> oldTableMap = new LinkedHashMap<String, TableBean>();
		oldList.forEach((t) -> oldTableMap.put(t.getTableName(), t));

		List<TableBean> addedTableList = new ArrayList<TableBean>();
		List<TableBean> deletedTableList = new ArrayList<TableBean>();

		for (TableBean newTb : newList) {
			if (!oldTableMap.containsKey(newTb.getTableName())) {
				addedTableList.add(newTb);
			}
		}
		for (TableBean oldTb : oldList) {
			if (!newTableMap.containsKey(oldTb.getTableName())) {
				deletedTableList.add(oldTb);
			}
		}
		addedTableList.forEach(t -> LOGGER.info("新增表：" + t.getTableName() + ":" + t.getTableFullName()));
		deletedTableList.forEach(t -> LOGGER.info("删除表：" + t.getTableName() + ":" + t.getTableFullName()));
		LOGGER.info("表变更：");
		Map<String, ModifyTableInfo> modifyedTableMap = new LinkedHashMap<String, SRDMVerCheck.ModifyTableInfo>();
		for (TableBean newTb : newList) {
			TableBean oldTb = oldTableMap.get(newTb.getTableName());
			if (oldTb == null) {
				continue;
			}
			List<FieldBean> newFbList = newTb.getFieldList();
			List<FieldBean> oldFbList = oldTb.getFieldList();
			Map<String, FieldBean> newFieldMap = new LinkedHashMap<>();
			newFbList.forEach((t) -> newFieldMap.put(t.getFieldName(), t));
			Map<String, FieldBean> oldFieldMap = new LinkedHashMap<>();
			oldFbList.forEach((t) -> oldFieldMap.put(t.getFieldName(), t));

			List<FieldBean> addedFieldList = new ArrayList<>();
			List<FieldBean> deletedFieldList = new ArrayList<>();
			List<FieldBean[]> modifyedFieldList = new ArrayList<>();

			for (FieldBean newFb : newFbList) {
				if (!oldFieldMap.containsKey(newFb.getFieldName())) {
					addedFieldList.add(newFb);
				}
			}
			for (FieldBean oldFb : oldFbList) {
				if (!newFieldMap.containsKey(oldFb.getFieldName())) {
					deletedFieldList.add(oldFb);
				}
			}

			for (int i = 0; i < addedFieldList.size(); i++) {
				for (int j = 0; j < deletedFieldList.size(); j++) {
					if (addedFieldList.get(i).getFieldFullName().equals(deletedFieldList.get(j).getFieldFullName())) {
						modifyedFieldList.add(new FieldBean[] { deletedFieldList.get(j), addedFieldList.get(i) });
						addedFieldList.remove(i);
						deletedFieldList.remove(j);
						i--;
						j--;
						break;
					}
				}
			}
			if (!addedFieldList.isEmpty() || !deletedFieldList.isEmpty() || !modifyedFieldList.isEmpty()) {
				LOGGER.info("---------------------" + newTb.getTableName() + ":" + newTb.getTableFullName()
						+ "---------------------");
				addedFieldList.forEach(t -> LOGGER.info("新增字段：" + t.getFieldName() + ":" + t.getFieldFullName()));
				deletedFieldList.forEach(t -> LOGGER.info("删除字段：" + t.getFieldName() + ":" + t.getFieldFullName()));
				modifyedFieldList.forEach(t -> LOGGER.info("物理名变更字段：" + t[0].getFieldName() + " -> "
						+ t[1].getFieldName() + "" + ":" + t[0].getFieldFullName()));
				ModifyTableInfo dto = new ModifyTableInfo();
				dto.tb = newTb;
				dto.addedFieldList.addAll(addedFieldList);
				dto.deletedFieldList.addAll(deletedFieldList);
				dto.modifyedFieldList.addAll(modifyedFieldList);
				modifyedTableMap.put(newTb.getTableName(), dto);
			}
		}

		LOGGER.writeToFile(new File("target/dm_check.txt"));
	}

	private static List<TableBean> loadTableListFromDM() throws IOException {
		List<TableBean> tableList = new ArrayList<TableBean>();
		List<String> dmList = FileUtil.listFileNames(new File(wpPath, "dm/TABLE"), new String[] { ".wprx" });
		for (String dmPath : dmList) {
			String dmText = FileUtil.readString(dmPath);
			String tmp = dmText;
			String dmCode = StringUtils.substringBetween(tmp, "<code>", "</code>");
			String dmName = StringUtils.substringBetween(tmp, "<name>", "</name>");
			int index = -1;
			TableBean tb = new TableBean();
			tb.setTableName(dmCode);
			tb.setTableFullName(dmName);
			List<FieldBean> fieldList = new ArrayList<FieldBean>();
			while ((index = tmp.indexOf("<dm-item>")) != -1) {
				tmp = tmp.substring(index + "<dm-item>".length());
				int endIndex = tmp.indexOf("</dm-item>");
				String dmItem = tmp.substring(0, endIndex);
				String dmItemCode = StringUtils.substringBetween(dmItem, "<code>", "</code>");
				String dmItemName = StringUtils.substringBetween(dmItem, "<name>", "</name>");
				dmItemName = StringEscapeUtils.unescapeXml(dmItemName);
				FieldBean fb = new FieldBean();
				fb.setFieldName(dmItemCode);
				fb.setFieldFullName(dmItemName);
				fieldList.add(fb);
				tmp = tmp.substring(endIndex + "</dm-item>".length());
			}
			tb.setFieldList(fieldList);
			tableList.add(tb);
		}
		return tableList;
	}

}
