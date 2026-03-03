package cbai.tool.sr;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import cbai.util.FileUtil;
import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.dbaccess.DBAccess;
import cbai.util.dbaccess.DBConnection;
import cbai.util.log.UtilLogger;

public class SRDBVerCompare {
	private static final String wpPath = "D:\\MyDeveloper\\eclipse-workspace-sr\\sirius_rt-web-app-local";
	private static final UtilLogger LOGGER = new UtilLogger();
	private static DBConnection db = null;
	private static List<String> ids;
	static {
		try {
            ids = FileUtil.readAllLines("./neusoft_id.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	public static class ModifyTableInfo {
		public TableBean tb;
		public List<FieldBean> addedFieldList = new ArrayList<>();
		public List<FieldBean> deletedFieldList = new ArrayList<>();
		public List<FieldBean[]> modifyedFieldList = new ArrayList<>();
	}

	public static DBConnection getDBAccess() throws Exception {
		if (db == null) {
			Properties pro = new Properties();
			pro.load(DBAccess.class.getClassLoader().getResourceAsStream("jdbc.properties"));
			DBAccess access = new DBAccess(pro);
			db = access.getConnection();
		}
		return db;
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws SQLException, IOException {
		List<TableBean> newList = (List<TableBean>) FileUtil.readObjectFromFile(new File("./db.cache"));
		List<TableBean> oldList = (List<TableBean>) FileUtil.readObjectFromFile(new File("./db3.5.cache"));
		Map<String, TableBean> newTableMap = new LinkedHashMap<String, TableBean>();
		newList.forEach((t) -> newTableMap.put(t.getTableName(), t));
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
		addedTableList.forEach(t -> System.out.println("新增表：" + t.getTableName() + "," + t.getTableFullName()));
		deletedTableList.forEach(t -> System.out.println("删除表：" + t.getTableName() + "," + t.getTableFullName()));
		System.out.println("表变更：");
		Map<String, ModifyTableInfo> modifyedTableMap = new LinkedHashMap<String, SRDBVerCompare.ModifyTableInfo>();
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
				System.out.println("---------------------" + newTb.getTableName() + "," + newTb.getTableFullName()
						+ "---------------------");
				addedFieldList
						.forEach(t -> System.out.println("新增字段：" + t.getFieldName() + "," + t.getFieldFullName()));
				deletedFieldList
						.forEach(t -> System.out.println("删除字段：" + t.getFieldName() + "," + t.getFieldFullName()));
				modifyedFieldList.forEach(t -> System.out.println("物理名变更字段：" + t[0].getFieldName() + " -> "
						+ t[1].getFieldName() + "," + t[0].getFieldFullName()));
				ModifyTableInfo dto = new ModifyTableInfo();
				dto.tb = newTb;
				dto.addedFieldList.addAll(addedFieldList);
				dto.deletedFieldList.addAll(deletedFieldList);
				dto.modifyedFieldList.addAll(modifyedFieldList);
				modifyedTableMap.put(newTb.getTableName(), dto);
			}
		}

		checkIO(newTableMap, deletedTableList, modifyedTableMap);
		checkQuery(newTableMap, deletedTableList, modifyedTableMap);

		LOGGER.writeToFile(new File("target/io_query_check.txt"));

		if (db != null) {
			db.close();
		}
	}

	private static final Pattern ioDmPattern = Pattern
			.compile("<dm-code>(\\w+)</dm-code>(\\r?\\n\\W*)<dm-item-code>(\\w+)</dm-item-code>");

	private static void checkIO(Map<String, TableBean> newTableMap, List<TableBean> deletedTableList,
			Map<String, ModifyTableInfo> modifyedTableMap) throws IOException {

		List<String> ioList = FileUtil.listFileNames(new File(wpPath, "io"), new String[] { ".wprx" });
		for (String ioPath : ioList) {
			if (!isNeusoftMade(ioPath)) {
				continue;
			}
			String ioText = FileUtil.readString(ioPath);
			Matcher m = ioDmPattern.matcher(ioText);
			while (m.find()) {
				String filename = FileUtil.getFileName(ioPath);
				String dmCode = m.group(1);
				String dmItemCode = m.group(3);

				long count = deletedTableList.stream().filter((t) -> t.getTableName().equals(dmCode)).count();
				if (count > 0) {
					LOGGER.error("[" + filename + "]:" + "使用删除的表：" + dmCode + "," + dmItemCode);
					continue;
				}
				if (dmCode.startsWith("QY_") || dmCode.startsWith("WKDM_")) {
					continue;
				}

				if (!newTableMap.containsKey(dmCode)) {
					LOGGER.error("[" + filename + "]:" + "不存在的DM：" + dmCode);
					continue;
				}

				if (!modifyedTableMap.containsKey(dmCode)) {
					continue;
				}
				ModifyTableInfo modifyedTableInfo = modifyedTableMap.get(dmCode);

				long delFbCount = modifyedTableInfo.deletedFieldList.stream()
						.filter((t) -> t.getFieldName().equals(dmItemCode)).count();
				if (delFbCount > 0) {
					LOGGER.error("[" + filename + "]:" + "删除的DM字段：" + dmCode + "." + dmItemCode);
					continue;
				}

				long modifyedFbCount = modifyedTableInfo.modifyedFieldList.stream()
						.filter((t) -> t[0].getFieldName().equals(dmItemCode)).count();
				if (modifyedFbCount > 0) {
					LOGGER.error("[" + filename + "]:" + "变更的DM字段：" + dmCode + "." + dmItemCode);
					continue;
				}
			}
		}
	}

	private static boolean isNeusoftMade(String path) {
		for (String id : ids) {
			if (path.contains(id)) {
				return true;
			}
		}
		return false;
	}

	private static void checkQuery(Map<String, TableBean> newTableMap, List<TableBean> deletedTableList,
			Map<String, ModifyTableInfo> modifyedTableMap) throws IOException {
		List<String> qyList = FileUtil.listFileNames(new File(wpPath, "dm/QUERY"), new String[] { ".wprx" });
		for (String qyPath : qyList) {
			if (!isNeusoftMade(qyPath)) {
				continue;
			}
			String qyText = FileUtil.readString(qyPath);
			String tmp = qyText;
			int index = -1;
			while ((index = tmp.indexOf("<dm-prop>")) != -1) {
				tmp = tmp.substring(index + "<dm-prop>".length());
				int endIndex = tmp.indexOf("</dm-prop>");
				String dmProp = tmp.substring(0, endIndex);
				String key = StringUtils.substringBetween(dmProp, "<key>", "</key>");
				String value = StringUtils.substringBetween(dmProp, "<value>", "</value>");
				value = StringEscapeUtils.unescapeXml(value);
				tmp = tmp.substring(endIndex + "</dm-prop>".length());
				testQuerySql(qyPath, key, value);
			}
		}
	}

	private static void testQuerySql(String qyPath, String key, String value) {
		String testSql = value;
		testSql = testSql.replaceAll("@WP_AGGREGATE", " '' ");
		testSql = testSql.replaceAll("@WP_CONDITION", " 1 != 1 ");
		testSql = testSql.replaceAll("@\\d+", " '' ");
		testSql = testSql.replaceAll(":\\w+", " '' ");
		System.out.println(testSql);
		try {
			List<Map<String, Object>> resultList = getDBAccess().executeQuery(testSql);
			System.out.println(resultList.size());
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("[" + FileUtil.getFileName(qyPath) + "]:" + key + "->" + e.getMessage()
					+ "\r\n                              SQL:" + value.replaceAll("[\r\n]", " ")
					+ "\r\n                              TEST_SQL:" + testSql.replaceAll("[\r\n]", " "));
		}
	}
}
