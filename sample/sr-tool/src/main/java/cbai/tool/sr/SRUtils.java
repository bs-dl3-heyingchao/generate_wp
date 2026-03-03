package cbai.tool.sr;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cbai.util.FileUtil;
import cbai.util.db.define.TableBean;
import cbai.util.db.define.reader.ITableBeanReader;
import cbai.util.morphem.MorphemHelper;

public class SRUtils {
	private static MorphemHelper helper = null;
	private static SRSqlConvert sqlConvert = null;

	public synchronized static MorphemHelper getMorphemHelper() throws IOException {
		if (helper == null) {
			helper = new MorphemHelper(SRDictCreator.getDictList());
		}
		return helper;
	}

	@SuppressWarnings("unchecked")
	public static List<TableBean> getTableList() {
		File cahceFile = new File("./db.cache");
		List<TableBean> list = null;
		if (!cahceFile.exists()) {
			String excelDir = "./db/第４．０版";
			ITableBeanReader reader = new SRTableBeanReader(excelDir);
			list = reader.readTableList();
			FileUtil.writeObjectToFile(list, cahceFile);
		} else {
			list = (List<TableBean>) FileUtil.readObjectFromFile(cahceFile);
		}
		return list;
	}

	public static SRSqlConvert getSqlConvert() {
		if (sqlConvert == null) {
			List<TableBean> tableList = getTableList();
			sqlConvert = new SRSqlConvert(tableList);
		}
		return sqlConvert;
	}
}
