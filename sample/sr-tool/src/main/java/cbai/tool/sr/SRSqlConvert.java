package cbai.tool.sr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import cbai.util.FileUtil;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.sqlconvert.SqlConverterAbstract;

public class SRSqlConvert extends SqlConverterAbstract {

	public SRSqlConvert(List<TableBean> tableList) {
		super(tableList);
	}

	@Override
	public Map<String, String> prepareLines(List<String> arg0) {
		return null;
	}

	@Override
	public FieldBean getFieldFromDBName(String tableFullName, String fieldFullName) {
		FieldBean fb = super.getFieldFromDBName(tableFullName, fieldFullName);
		if (fb == null && fieldFullName.endsWith("No")) {
			String tmp = fieldFullName.substring(0, fieldFullName.length() - 2) + "№";
			fb = super.getFieldFromDBName(tableFullName, tmp);
		}
		return fb;
	}

	public void addTableBean(TableBean... tableBeans) {
		Map<String, TableBean> tableMap = this.getTableMap();
		for (TableBean tb : tableBeans) {
			tableMap.put(tb.getTableFullName(), tb);
		}
	}

	public static void main(String[] args) throws IOException {
		List<TableBean> tableList = SRUtils.getTableList();
		SRSqlConvert sqlConvert = new SRSqlConvert(tableList);
		System.out.println(sqlConvert.convert(FileUtil.readString("./input.sql")));
	}
}
