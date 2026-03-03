package cbai.tool.sr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.EmptyFileException;
import org.apache.poi.ss.usermodel.Sheet;

import cbai.tool.sr.bean.SRクエリー定義書Bean;
import cbai.tool.sr.bean.SRクエリー定義書Bean.SRクエリー定義書ItemBean;
import cbai.tool.sr.bean.SR対象条件Bean;
import cbai.tool.sr.bean.SR画面チェック仕様書Bean;
import cbai.tool.sr.bean.SR画面チェック仕様書Bean.SRチェックItemBean;
import cbai.tool.sr.bean.SR画面項目説明書Bean;
import cbai.tool.sr.bean.SR詳細設計Bean;
import cbai.util.FileUtil;
import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.db.define.TableBean.TABLE_TYPE;
import cbai.util.excel.ExcelUtil;
import cbai.util.log.UtilLogger;
import cbai.util.sqlconvert.SqlConverterAbstract;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SR詳細設計Reader {
	private String excelPath;
	private ExcelUtil excelUtil;
	private SR詳細設計Bean sr詳細設計Bean;
	private SqlConverterAbstract sqlConvert;
	private UtilLogger logger = null;

	public SR詳細設計Reader(String excelPath, SqlConverterAbstract sqlConvert) {
		this.excelPath = excelPath;
		this.sqlConvert = sqlConvert;
	}

	public void setLogger(UtilLogger logger) {
		this.logger = logger;
	}

	public void read() throws IOException {
		this.excelUtil = new ExcelUtil(this.excelPath);
		this.sr詳細設計Bean = new SR詳細設計Bean();
		for (int sheetIndex = 0; sheetIndex < excelUtil.getNumberOfSheets(); sheetIndex++) {
			Sheet sheet = excelUtil.getSheetAt(sheetIndex);
			if (!sheet.getSheetName().equals("画面項目説明書") && sheet.getSheetName().contains("画面項目説明書")) {
				System.out.println("EXTEND:" + this.excelPath + "\t" + sheet.getSheetName());
			}
			if (sheet.getSheetName().equals("画面項目説明書")) {
				read画面項目説明書(sheet);
			} else if (sheet.getSheetName().equals("画面項目説明書 (エクスポート)")
					|| (sheet.getSheetName().startsWith("画面項目説明書") || sheet.getSheetName().contains("CSV"))) {
				read画面項目説明書CSV(sheet);
			} else if (sheet.getSheetName().equals("画面定義書")) {
				read画面定義書(sheet);
			} else if (sheet.getSheetName().startsWith("クエリー定義書")) {
				readクエリー定義書(sheet);
			} else if (sheet.getSheetName().equals("画面チェック仕様書")) {
				read画面チェック仕様書(sheet);
			}
		}
		this.excelUtil.close();
	}

	private String readRow(Sheet sheet, int rowIndex, int fromCol, int toCol) {
		StringBuilder sb = new StringBuilder();
		for (int i = fromCol; i <= toCol; i++) {
			String value = excelUtil.getCellStringValueWithoutDel(sheet, i, rowIndex).trim().trim();
			sb.append(" ").append(value);
		}
		return sb.toString().trim();
	}

	private void read画面チェック仕様書(Sheet sheet) {
		String groupName = "";
		for (int row = 5; row <= sheet.getLastRowNum(); row++) {
			String no = excelUtil.getCellStringValueWithoutDel(sheet, 0, row).trim();
			if (no.startsWith("入力チェック")) {
				groupName = no;
				if (this.sr詳細設計Bean.画面チェック仕様書 == null) {
					this.sr詳細設計Bean.画面チェック仕様書 = new SR画面チェック仕様書Bean();
				}
				if (this.sr詳細設計Bean.画面チェック仕様書.listチェックItemMap == null) {
					this.sr詳細設計Bean.画面チェック仕様書.listチェックItemMap = new LinkedHashMap<String, List<SRチェックItemBean>>();
				}
				this.sr詳細設計Bean.画面チェック仕様書.listチェックItemMap.put(groupName, new ArrayList<SRチェックItemBean>());
			} else {
				if (!no.matches("\\d+")) {
					continue;
				}
				List<SRチェックItemBean> list = this.sr詳細設計Bean.画面チェック仕様書.listチェックItemMap.get(groupName);
				SRチェックItemBean item = new SRチェックItemBean();
				item.項番 = no;
				item.チェック名 = excelUtil.getCellStringValueWithoutDel(sheet, 1, row).trim();
				item.仕様説明 = excelUtil.getCellStringValueWithoutDel(sheet, 12, row).trim();
				item.チェックアクション = excelUtil.getCellStringValueWithoutDel(sheet, 40, row).trim();
				item.メッセージID = excelUtil.getCellStringValueWithoutDel(sheet, 48, row).trim();
				item.メッセージ内容 = excelUtil.getCellStringValueWithoutDel(sheet, 53, row).trim();
				list.add(item);
			}
		}
	}

	private void read画面定義書(Sheet sheet) {
		this.sr詳細設計Bean.画面ID = excelUtil.getCellStringValueWithoutDel(sheet, 18, 2);
		this.sr詳細設計Bean.画面名 = excelUtil.getCellStringValueWithoutDel(sheet, 30, 2);
		boolean conditionStart = false;
		boolean joinStart = false;
		for (int row = 4; row <= sheet.getLastRowNum(); row++) {
			String tag = excelUtil.getCellStringValueWithoutDel(sheet, 48, row).trim();
			if ("対象条件".equals(tag)) {
				this.sr詳細設計Bean.対象条件 = new SR対象条件Bean();
				this.sr詳細設計Bean.対象条件.対象データモデル = readRow(sheet, row + 1, 48, 67);
				this.sr詳細設計Bean.対象条件.対象条件 = "";
				row++;
				conditionStart = true;
			} else if (tag.startsWith("親子関係の紐づけ")) {
				conditionStart = false;
				joinStart = true;
				this.sr詳細設計Bean.対象条件.親子関係の紐づけ = "";
				break;
			} else if ("対象データモデル".equals(tag) || "論理名称".equals(tag)) {
				break;
			} else if (conditionStart) {
				String line = readRow(sheet, row, 48, 67);
				if (StringUtils.isNotEmpty(line)) {
					this.sr詳細設計Bean.対象条件.対象条件 += readRow(sheet, row, 48, 67) + "\n";
				}
			} else if (joinStart) {
				String line = readRow(sheet, row, 48, 67);
				if (StringUtils.isNotEmpty(line)) {
					this.sr詳細設計Bean.対象条件.親子関係の紐づけ += readRow(sheet, row, 48, 67) + "\n";
				}
			}
		}
	}

	public void close() {
		this.excelUtil.close();
	}

	public List<TableBean> getQueryTableBeans() {
		List<TableBean> tableList = new ArrayList<TableBean>();
		if (sr詳細設計Bean.listクエリー定義書 != null) {
			for (SRクエリー定義書Bean queryBean : sr詳細設計Bean.listクエリー定義書) {
				TableBean tb = new TableBean();
				tb.setTableName(queryBean.データモデル);
				if (sqlConvert.findTableItem(queryBean.クエリー名, null).isFind()) {
					writeErrorLog("クエリー定義書 データモデル[{}] クエリー名 [{}] が既に存在しています", queryBean.データモデル, queryBean.クエリー名);
					tb.setTableFullName(queryBean.クエリー名 + System.currentTimeMillis());
				} else {
					tb.setTableFullName(queryBean.クエリー名);
				}
				tb.setTableType(TABLE_TYPE.VIEW);
				List<FieldBean> fieldList = new ArrayList<FieldBean>();
				if (queryBean.listクエリー定義書Item != null) {
					for (SRクエリー定義書ItemBean queryItem : queryBean.listクエリー定義書Item) {
						FieldBean fb = new FieldBean();
						FieldBean srcFb = sqlConvert.getFieldFromDBName(queryItem.テーブル名, queryItem.カラム名);
						if (srcFb != null) {
							try {
								BeanUtils.copyProperties(fb, srcFb);
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							fb.setFieldName(queryItem.名称);
						}
						fb.setFieldFullName(queryItem.名称);
						fieldList.add(fb);
					}
				}
				tb.setFieldList(fieldList);
				tableList.add(tb);
			}
		}
		return tableList;
	}

	private void writeErrorLog(String format, Object... arguments) {
		String logPrefix = String.format("[%s:%s] ", sr詳細設計Bean.画面ID, sr詳細設計Bean.画面名);
		log.error(logPrefix + format, arguments);
		if (logger != null) {
			logger.error(logPrefix + format, arguments);
		}
	}

	private void writeWarnLog(String format, Object... arguments) {
		String logPrefix = String.format("[%s:%s] ", sr詳細設計Bean.画面ID, sr詳細設計Bean.画面名);
		log.warn(logPrefix + format, arguments);
		if (logger != null) {
			logger.warn(logPrefix + format, arguments);
		}
	}

	public void readクエリー定義書(Sheet sheet) {
		for (int row = 3; row <= sheet.getLastRowNum(); row++) {
			String queryTag = excelUtil.getCellStringValueWithoutDel(sheet, 0, row).trim();
			if ("データモデル".equals(queryTag)) {
				if (this.sr詳細設計Bean.listクエリー定義書 == null) {
					this.sr詳細設計Bean.listクエリー定義書 = new ArrayList<>();
				}
				SRクエリー定義書Bean queryBean = new SRクエリー定義書Bean();
				queryBean.データモデル = excelUtil.getCellStringValueWithoutDel(sheet, 6, row).trim().replace(" ", "");
				queryBean.クエリー名 = excelUtil.getCellStringValueWithoutDel(sheet, 30, row).trim().replace(" ", "");
				if (!queryBean.クエリー名.endsWith("クエリ")) {
					writeWarnLog("クエリー定義書 データモデル[{}]のクエリー名[{}]の後ろに「クエリ」を追加とします。", queryBean.データモデル, queryBean.クエリー名);
					queryBean.クエリー名 += "クエリ";
				}
				this.sr詳細設計Bean.listクエリー定義書.add(queryBean);
			} else {
				String index = excelUtil.getCellStringValueWithoutDel(sheet, 0, row).trim();
				String name = excelUtil.getCellStringValueWithoutDel(sheet, 2, row).trim();
				String dmType = excelUtil.getCellStringValueWithoutDel(sheet, 16, row).trim();
				if (!index.matches("\\d+") || StringUtils.isEmpty(name) || StringUtils.isEmpty(dmType)) {
					continue;
				}
				SRクエリー定義書ItemBean itemBean = new SRクエリー定義書ItemBean();
				itemBean.項番 = index;
				itemBean.名称 = name;
				itemBean.データ型 = excelUtil.getCellStringValueWithoutDel(sheet, 16, row).trim();
				itemBean.長さ = excelUtil.getCellStringValueWithoutDel(sheet, 20, row).trim();
				itemBean.テーブル名 = excelUtil.getCellStringValueWithoutDel(sheet, 23, row).trim();
				itemBean.カラム名 = excelUtil.getCellStringValueWithoutDel(sheet, 31, row).trim();
				itemBean.備考 = excelUtil.getCellStringValueWithoutDel(sheet, 41, row).trim();
				SRクエリー定義書Bean queryBean = this.sr詳細設計Bean.listクエリー定義書.get(this.sr詳細設計Bean.listクエリー定義書.size() - 1);
				if (queryBean.listクエリー定義書Item == null) {
					queryBean.listクエリー定義書Item = new ArrayList<>();
				}
				queryBean.listクエリー定義書Item.add(itemBean);
			}
		}
	}

	public void read画面項目説明書(Sheet sheet) {
		this.sr詳細設計Bean.list画面項目説明書 = new ArrayList<>();
		for (int row = 6; row <= sheet.getLastRowNum(); row++) {
			String index = excelUtil.getCellStringValueWithoutDel(sheet, 0, row).trim();
			if (!index.matches("\\d+")) {
				SR画面項目説明書Bean bean = new SR画面項目説明書Bean();
				this.sr詳細設計Bean.list画面項目説明書.add(bean);
				continue;
			}
			String itemName = excelUtil.getCellStringValueWithoutDel(sheet, 1, row).trim();
			if (StringUtils.isEmpty(itemName)) {
				continue;
			}
			SR画面項目説明書Bean bean = new SR画面項目説明書Bean();
			bean.項番 = index;
			bean.項目名 = itemName;
			bean.IO = excelUtil.getCellStringValueWithoutDel(sheet, 7, row).trim();
			bean.表示 = excelUtil.getCellStringValueWithoutDel(sheet, 9, row).trim();
			bean.属性 = excelUtil.getCellStringValueWithoutDel(sheet, 11, row).trim();
			bean.桁数 = excelUtil.getCellStringValueWithoutDel(sheet, 14, row).trim();
			bean.必須 = excelUtil.getCellStringValueWithoutDel(sheet, 17, row).trim();
			bean.ソート順 = excelUtil.getCellStringValueWithoutDel(sheet, 19, row).trim();
			bean.テーブル名 = excelUtil.getCellStringValueWithoutDel(sheet, 21, row).trim();
			bean.テーブル項目名 = excelUtil.getCellStringValueWithoutDel(sheet, 27, row).trim();
			bean.初期値 = excelUtil.getCellStringValueWithoutDel(sheet, 33, row).trim();
			bean.加工式 = excelUtil.getCellStringValueWithoutDel(sheet, 42, row).trim();
			bean.選択リスト = excelUtil.getCellStringValueWithoutDel(sheet, 51, row).trim();
			bean.表示条件 = excelUtil.getCellStringValueWithoutDel(sheet, 60, row).trim();
			bean.備考 = excelUtil.getCellStringValueWithoutDel(sheet, 69, row).trim();
			this.sr詳細設計Bean.list画面項目説明書.add(bean);
		}
	}

	public void read画面項目説明書CSV(Sheet sheet) {
		this.sr詳細設計Bean.list画面項目説明書CSV = new ArrayList<>();
		for (int row = 6; row <= sheet.getLastRowNum(); row++) {
			String index = excelUtil.getCellStringValueWithoutDel(sheet, 0, row).trim();
			if (!index.matches("\\d+")) {
				SR画面項目説明書Bean bean = new SR画面項目説明書Bean();
				this.sr詳細設計Bean.list画面項目説明書CSV.add(bean);
				continue;
			}
			String itemName = excelUtil.getCellStringValueWithoutDel(sheet, 1, row).trim();
			if (StringUtils.isEmpty(itemName)) {
				continue;
			}
			SR画面項目説明書Bean bean = new SR画面項目説明書Bean();
			bean.項番 = index;
			bean.項目名 = itemName;
			bean.IO = excelUtil.getCellStringValueWithoutDel(sheet, 7, row).trim();
			bean.表示 = excelUtil.getCellStringValueWithoutDel(sheet, 9, row).trim();
			bean.属性 = excelUtil.getCellStringValueWithoutDel(sheet, 11, row).trim();
			bean.桁数 = excelUtil.getCellStringValueWithoutDel(sheet, 14, row).trim();
			bean.必須 = excelUtil.getCellStringValueWithoutDel(sheet, 17, row).trim();
			bean.ソート順 = excelUtil.getCellStringValueWithoutDel(sheet, 19, row).trim();
			bean.テーブル名 = excelUtil.getCellStringValueWithoutDel(sheet, 21, row).trim();
			bean.テーブル項目名 = excelUtil.getCellStringValueWithoutDel(sheet, 27, row).trim();
			bean.初期値 = excelUtil.getCellStringValueWithoutDel(sheet, 33, row).trim();
			bean.加工式 = excelUtil.getCellStringValueWithoutDel(sheet, 42, row).trim();
			bean.選択リスト = excelUtil.getCellStringValueWithoutDel(sheet, 51, row).trim();
			bean.表示条件 = excelUtil.getCellStringValueWithoutDel(sheet, 60, row).trim();
			bean.備考 = excelUtil.getCellStringValueWithoutDel(sheet, 69, row).trim();
			this.sr詳細設計Bean.list画面項目説明書CSV.add(bean);
		}
	}

	public SR詳細設計Bean getSr詳細設計Bean() {
		return sr詳細設計Bean;
	}

	public static void main(String[] args) throws IOException {
		File inputDir = new File("C:\\Users\\bai.chen\\Downloads\\02_販売管理(HB)\\02-1_レンタル");
		List<String> docList = FileUtil.listFileNames(inputDir, new String[] { ".xlsx" });
		SqlConverterAbstract sqlConvert = SRUtils.getSqlConvert();
		for (String fn : docList) {
			if (fn.toLowerCase().contains("/bk/")) {
				continue;
			}
			File file = new File(fn);
			if (file.getName().startsWith("~$")) {
				continue;
			}
//			if (!file.getName().contains("SRTWHB1106")) {
//				continue;
//			}
			try {
				SR詳細設計Reader reader = new SR詳細設計Reader(fn, sqlConvert);
				reader.read();
//				SR詳細設計Bean result = reader.getSr詳細設計Bean();
//				System.out.println(result.画面チェック仕様書);
				reader.close();
			} catch (EmptyFileException e) {
				continue;
			}
		}
	}

}
