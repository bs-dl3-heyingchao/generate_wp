package cbai.tool.sr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.EmptyFileException;

import cbai.tool.sr.bean.ChoiceBean;
import cbai.tool.sr.bean.DmItem;
import cbai.tool.sr.bean.IOItem;
import cbai.tool.sr.bean.ItemProp;
import cbai.tool.sr.bean.SRクエリー定義書Bean;
import cbai.tool.sr.bean.SRクエリー定義書Bean.SRクエリー定義書ItemBean;
import cbai.tool.sr.bean.SR対象条件Bean;
import cbai.tool.sr.bean.SR画面項目説明書Bean;
import cbai.tool.sr.bean.SR詳細設計Bean;
import cbai.util.FileUtil;
import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.log.UtilLogger;
import cbai.util.morphem.MorphemHelper;
import cbai.util.sqlconvert.SqlConverterAbstract.FindedTableItem;
import cbai.util.template.CreateTemplateVelocity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SRWprxCreator {
	private final static CreateTemplateVelocity createTemplate = new CreateTemplateVelocity("cbai.tool.sr.WP_TEMPLATE",
			true);

	private final static String[][] ACTION_ARRAY = { { "検索", "SEARCH" }, { "登録", "INSERT" }, { "更新", "UPDATE" },
			{ "登録（更新）", "UPDATE" }, { "削除", "DELETE" }, { "新規", "CREATE" }, { "選択", "CHOICE" }, { "クリア", "CLEAR" },
			{ "次へ", "NEXT" }, { "一覧画面へ", "LIST" }, { "戻る", "BACK" }, { "閉じる", "CLOSE" }, { "インポート", "IMPORT" },
			{ "エクスポート", "EXPORT" }, { "帳票出力", "EXPORT" }, { "追加", "ADDITION" }, { "○○追加", "ADDITION" },
			{ "編集", "EDIT" }, { "確認", "PRE_PROPOSE" }, { "申請", "PROPOSE" }, { "○○申請", "PROPOSE" }, { "コピー", "COPY" },
			{ "複写", "COPY" }, { "失注", "LOST" }, { "再計算" }, { "RECALCULATION" }, { "引当要求", "RESERVE" },
			{ "照会", "QUERY" }, { "○○照会", "QUERY" }, { "状況", "STATUS" }, { "受領", "RECEIPT" }, { "○○受領", "RECEIPT" },
			{ "保管", "STORAGE" }, { "○○保管", "STORAGE" }, { "作業指示", "WORK" }, { "出荷指示", "SHIPPING" } };

	private final SR詳細設計Bean xxsjBean;
	private final SRSqlConvert sqlConvert;
	private final String logPrefix;
	private final MorphemHelper HELPER;
	private UtilLogger logger = null;

	public SRWprxCreator(SR詳細設計Bean sr詳細設計Bean, SRSqlConvert sqlConvert, MorphemHelper morphemHelper) {
		this.xxsjBean = sr詳細設計Bean;
		this.sqlConvert = sqlConvert;
		this.HELPER = morphemHelper;
		this.logPrefix = String.format("[%s:%s] ", xxsjBean.画面ID, xxsjBean.画面名);
	}

	public void setLogger(UtilLogger logger) {
		this.logger = logger;
	}

	private boolean hasValue(String value) {
		return StringUtils.isNotEmpty(value) && !"－".equals(value) && !"ー".equals(value);
	}

	public List<Map<String, Object>> getDmReplaceMaps() {
		List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
		if (xxsjBean.listクエリー定義書 != null) {
			for (SRクエリー定義書Bean queryBean : xxsjBean.listクエリー定義書) {
				Map<String, Object> replaceMap = new HashMap<String, Object>();
				if (queryBean.listクエリー定義書Item != null) {
					replaceMap.put("dmId", queryBean.データモデル);
					replaceMap.put("dmName", queryBean.クエリー名);
					List<DmItem> dmList = new ArrayList<DmItem>();
					for (SRクエリー定義書ItemBean queryItem : queryBean.listクエリー定義書Item) {
						DmItem dmItem = new DmItem();
						if ("DM".equals(queryItem.データ型)) {
							FieldBean srcFb = sqlConvert.getFieldFromDBName(queryItem.テーブル名, queryItem.カラム名);
							if (srcFb != null) {
								if (srcFb.getOthers() != null) {
									dmItem.data_type = srcFb.getOthers().get("WP_TYPE");
								}
								if (StringUtils.isEmpty(dmItem.data_type) || "FILE".equals(dmItem.data_type)) {
									if (StringUtils.isNotEmpty(srcFb.getType())) {
										if (srcFb.getType().contains("varchar")) {
											dmItem.data_type = "TEXT";
										} else if (srcFb.getType().contains("decimal")) {
											dmItem.data_type = "NUM";
										} else if (srcFb.getType().contains("image")) {
											dmItem.data_type = "FILE";
										}
									}
								}
								dmItem.code = srcFb.getFieldName();
								dmItem.name = queryItem.名称;
								dmItem.length = srcFb.getLen();
								dmItem.byteSize = "0";
								dmItem.scale = srcFb.getDotLen();
								dmItem.is_nullable = String.valueOf(!srcFb.isNotNull());
								dmItem.key_group = srcFb.isKey() ? "1" : "0";
							} else {
								writeErrorLog("クエリー定義書 データモデル[{}] 項番[{}] テーブルやカラムが見つかりません:{}.{}", queryBean.データモデル,
										queryItem.項番, queryItem.テーブル名, queryItem.カラム名);
								dmItem.data_type = "TEXT";
								dmItem.code = queryItem.名称;
								dmItem.name = queryItem.名称;
							}
						} else {
							dmItem.data_type = queryItem.データ型;
							String len = queryItem.長さ;
							if (StringUtils.isNotEmpty(len)) {
								String[] ary = len.split(",", -1);
								if (ary.length == 2) {
									dmItem.length = ary[0];
									dmItem.scale = ary[1];
								} else {
									dmItem.length = queryItem.長さ;
								}
							}
							dmItem.code = queryItem.名称;
							dmItem.name = queryItem.名称;
						}
						dmList.add(dmItem);
					}
					replaceMap.put("dmItemList", dmList);
					listMap.add(replaceMap);
				} else {
					System.out.println(queryBean);
				}
			}
		}
		return listMap;
	}

	public Map<String, Object> getIoReplaceMap(String parentDir, String ioType) {
		Map<String, Object> replaceMap = new HashMap<String, Object>();
		String ioSuffix = "";
		String subLogPrefix = "";
		List<SR画面項目説明書Bean> list = null;
		if ("IO".equals(ioType)) {
			ioSuffix = "IO";
			list = xxsjBean.list画面項目説明書;
			subLogPrefix = "画面項目説明書 ";
		} else if ("EXPORT".equals(ioType)) {
			ioSuffix = "EX";
			list = xxsjBean.list画面項目説明書CSV;
			subLogPrefix = "画面項目説明書 (エクスポート) ";
		} else {
			throw new IllegalArgumentException();
		}
		replaceMap.put("io_type", ioType);
		replaceMap.put("gmId", xxsjBean.画面ID);
		replaceMap.put("gmIoId", xxsjBean.画面ID.substring(4) + ioSuffix);
		replaceMap.put("gmName", xxsjBean.画面名);
		replaceMap.put("parentDir", parentDir);
		SR対象条件Bean dxtjBean = xxsjBean.対象条件;
		if (dxtjBean != null) {
			if (StringUtils.isNotEmpty(dxtjBean.対象データモデル)) {
				String[] array = dxtjBean.対象データモデル.split(" +");
				String gmDmName = array[0];
				String gmDmCode = "";
				if (array.length > 1) {
					gmDmCode = array[1];
				} else {
					FindedTableItem findedTableItem = sqlConvert.findTableItem(gmDmName, null);
					if (findedTableItem.isFind()) {
						TableBean tb = findedTableItem.getTableBean();
						gmDmCode = tb.getTableName();
					} else {
						writeErrorLog(subLogPrefix + "対象条件DMが見つかりません:{}", gmDmName);
					}
				}
				replaceMap.put("gmDmCode", gmDmCode);
				String gmIoCondition = dxtjBean.対象条件;
				if (StringUtils.isNotEmpty(gmIoCondition)) {
					gmIoCondition = gmIoCondition.replace("(完全一致)", "");
					gmIoCondition = gmIoCondition.replace("パラメータ @", "@");
					gmIoCondition = gmIoCondition.replace("＝", "=");
					gmIoCondition = gmIoCondition.replace("かつ", " and ");
					gmIoCondition = gmIoCondition.replace("　", " ");
					gmIoCondition = gmIoCondition.replace("’", "'");
					gmIoCondition = gmIoCondition.replace("’", "'");
					gmIoCondition = gmIoCondition.replace("\n", " ");
					Matcher m = CONDTION_PATTERN.matcher(gmIoCondition);
					StringBuffer sb = new StringBuffer();
					while (m.find()) {
						String filedFullName = m.group(1).trim();
						FieldBean fb = sqlConvert.getFieldFromDBName(gmDmName, filedFullName);
						if (fb == null) {
							writeErrorLog(subLogPrefix + "対象条件が見つかりません:{}.{}", gmDmName, filedFullName);
							continue;
						}
						m.appendReplacement(sb, fb.getFieldName() + "=");
					}
					m.appendTail(sb);
					gmIoCondition = sb.toString().trim();
				}
				replaceMap.put("gmIoCondition", escapseXml(gmIoCondition));
			}
		}
		Map<String, List<IOItem>> itemNameMap = new HashMap<String, List<IOItem>>();
		Set<String> codeSet = new HashSet<String>();
		List<IOItem> ioItemList = new ArrayList<>();
		int groupIndex = 0;
		boolean isInGroup = false;
		String curGroupPrefix = "";
		for (SR画面項目説明書Bean itemBean : list) {
			if (StringUtils.isEmpty(itemBean.項番)) {
				isInGroup = false;
				curGroupPrefix = "";
				continue;
			}
			IOItem ioItem = new IOItem();
			ioItem.io_code = xxsjBean.画面ID;
			ioItem.name = itemBean.項目名;
			String display = itemBean.表示;
			String io = itemBean.IO;
			String codePrefix = "D_";
			if (io.contains("I入力")) {
				ioItem.item_type = "I";
//				codePrefix = "I_";
			} else if (io.contains("IO入出力")) {
				ioItem.item_type = "IO";
//				codePrefix = "IO_";
			} else if (io.contains("O出力")) {
				ioItem.item_type = "O";
//				codePrefix = "O_";
			} else if (io.contains("Aアクション")) {
				ioItem.item_type = "A";
				codePrefix = "A_";
			} else if (io.contains("Gグループ")) {
				ioItem.level = "1";
				ioItem.item_type = "G";
				groupIndex++;
				isInGroup = true;
				codePrefix = "G" + groupIndex + "_";
				curGroupPrefix = "G" + groupIndex + "_";
			} else {
				writeErrorLog(subLogPrefix + "項番[{}] unkonw I/O :{}", itemBean.項番, io);
			}
			if (display.contains("非表示")) {
				ioItem.is_visible = "false";
				codePrefix = "H_";
			} else {
				ioItem.is_visible = "true";
			}
			if (isInGroup && !io.contains("Gグループ")) {
				codePrefix = curGroupPrefix + codePrefix;
				ioItem.level = "2";
			}
			if (itemBean.桁数.matches("\\d+")) {
				ioItem.length = itemBean.桁数;
			}

			// 備考 備考によりラベルが変更する可能性があるので、先に解析する必要があります
			setBiko(itemBean, ioItem);

			// 対象テーブル情報
			String tableFullName = itemBean.テーブル名.replaceAll("[\r\n]", "");
			String fieldFullName = itemBean.テーブル項目名.replaceAll("[\r\n]", "");
			if (hasValue(tableFullName) /* && !tableFullName.endsWith("クエリ") */) {
				FindedTableItem findedTableItem = sqlConvert.findTableItem(itemBean.テーブル名, null);
				if (findedTableItem.isFind()) {
					TableBean tb = findedTableItem.getTableBean();
					if ("DM".equals(itemBean.属性)) {
						ioItem.dm_code = tb.getTableName();
					}
					FieldBean fb = sqlConvert.getFieldFromDBName(tableFullName, fieldFullName);
					if (fb != null) {
						if ("DM".equals(itemBean.属性)) {
							ioItem.dm_item_code = fb.getFieldName();
						}
						ioItem.code = fb.getFieldName();
					} else {
						ioItem.dm_item_code = fieldFullName;
						writeErrorLog(subLogPrefix + "項番[{}] テーブル項目が見つかりません:{}.{}", itemBean.項番, tableFullName,
								fieldFullName);
					}
				} else {
					ioItem.dm_code = itemBean.テーブル名;
					writeErrorLog(subLogPrefix + "項番[{}] テーブルが見つかりません:{}", itemBean.項番, tableFullName);
				}
			} else {
				if ("A".equals(ioItem.item_type)) {
					ioItem.code = getActionIoCode(itemBean.項目名, ioItem.label);
				}
				if (StringUtils.isEmpty(ioItem.code)) {
					String id = HELPER.getRomaFromKanji(itemBean.項目名).toUpperCase();
					ioItem.code = id;
				}
			}
			if (!"DM".equals(itemBean.属性)) {
				if (StringUtils.isNotEmpty(itemBean.属性) && itemBean.属性.length() > 1) {
					ioItem.dm_item_code = "@" + itemBean.属性;
				} else {
					if (ioItem.item_type.contains("I") || ioItem.item_type.contains("O")) {
						ioItem.dm_item_code = "@TEXT";
					}
				}
			}
			if (!"G".equals(ioItem.item_type)
					&& StringUtils.isNotEmpty(ioItem.code) /* && !"DM".equals(itemBean.属性) */) {
				ioItem.code = codePrefix + ioItem.code;
				while (!codeSet.add(ioItem.code)) {
					if (ioItem.code.matches(".*_\\d+$")) {
						try {
							String part1 = ioItem.code.substring(0, ioItem.code.lastIndexOf("_"));
							String part2 = ioItem.code.substring(ioItem.code.lastIndexOf("_") + 1);
							int suffixLen = part2.length();
							int index = Integer.parseInt(part2);
							suffixLen = Math.max(suffixLen, String.valueOf(index + 1).length());
							ioItem.code = part1 + "_"
									+ StringUtils.leftPad(String.valueOf((index + 1)), suffixLen, '0');
						} catch (Exception e) {
							ioItem.code = ioItem.code + "_1";
						}
					} else {
						if ("A".equals(ioItem.item_type)) {
							ioItem.code = ioItem.code + "_01";
						} else {
							ioItem.code = ioItem.code + "_1";
						}
					}
					ioItem.code = ioItem.code.replaceAll("_+", "_");
				}
			}
			// 初期値
			ioItem.default_value = getInitValue(subLogPrefix, itemBean);
			// 加工式
			ioItem.statement = getStatement(subLogPrefix, itemBean);
			// 選択リスト
			ioItem.choiceInfo = getChoiceInfo(subLogPrefix, itemBean);
			// 表示条件
			ioItem.condition = getCondition(subLogPrefix, itemBean);
			ioItemList.add(ioItem);
			if (StringUtils.isNotEmpty(ioItem.name)) {
				if (!itemNameMap.containsKey(ioItem.name)) {
					itemNameMap.put(ioItem.name, new ArrayList<IOItem>());
				}
				itemNameMap.get(ioItem.name).add(ioItem);
			}
		}
		if (xxsjBean.画面チェック仕様書 != null && xxsjBean.画面チェック仕様書.listチェックItemMap != null) {
			Map<String, Integer> actionIndexMap = new HashMap<String, Integer>();
			xxsjBean.画面チェック仕様書.listチェックItemMap.forEach((key, value) -> {
				if (key.contains("IO")) {
					value.forEach((checkItem) -> {
						IOItem ioItem = new IOItem();
						ioItem.io_code = xxsjBean.画面ID;
						ioItem.name = escapseXml(checkItem.チェックアクション.trim().replace("\n", "／") + " " + checkItem.チェック名);
						ioItem.is_visible = "false";
						ioItem.item_type = "C";
						String codePrefix = "C_";
						String[] actions = checkItem.チェックアクション.trim().split("\n");
//						if (actions.length == 1 && itemNameMap.containsKey(actions[0])) {
//							String action = actions[0];
//							if (!actionIndexMap.containsKey(action)) {
//								actionIndexMap.put(action, 0);
//							}
//							List<IOItem> actionItems = itemNameMap.get(action);
//							for (IOItem item : actionItems) {
//								if ("A".equals(item.item_type) && item.code.startsWith("A_")) {
//									int index = actionIndexMap.get(action) + 1;
//									String code = item.code.substring(2) + "_"
//											+ StringUtils.leftPad(String.valueOf(index), 2, '0');
//									actionIndexMap.put(action, index);
//									ioItem.code = code;
//									break;
//								}
//							}
//						} else
						StringBuilder actionConditionSb = new StringBuilder();
						if (actions.length > 0) {
							String actionKey = StringUtils.join(actions, "_");
							if (!actionIndexMap.containsKey(actionKey)) {
								actionIndexMap.put(actionKey, 0);
							}
							StringBuilder sb = new StringBuilder();
							for (String action : actions) {
								List<IOItem> actionItems = itemNameMap.get(action);
								String name = "";
								if (actionItems != null) {
									for (IOItem item : actionItems) {
										if ("A".equals(item.item_type) && item.code != null
												&& item.code.startsWith("A_")) {
											name = item.code.substring(2);
											break;
										}
									}
								}
								if (sb.length() > 0) {
									sb.append("_");
								}
								if (StringUtils.isEmpty(name)) {
									name = action;
								}
								sb.append(name);
								if (actionConditionSb.length() > 0) {
									actionConditionSb.append(" OR ");
								}
								actionConditionSb.append(String.format("@ACTION = '%s'", "A_" + name));
							}
							String code = sb.toString();
							int index = actionIndexMap.get(actionKey) + 1;
							code = code + "_" + StringUtils.leftPad(String.valueOf(index), 2, '0');
							actionIndexMap.put(actionKey, index);
							ioItem.code = code;
						}
						if (StringUtils.isEmpty(ioItem.code)) {
							String action = "IO";
							if (!actionIndexMap.containsKey(action)) {
								actionIndexMap.put(action, 0);
							}
							int index = actionIndexMap.get(action) + 1;
							String code = action + "_" + StringUtils.leftPad(String.valueOf(index), 2, '0');
							actionIndexMap.put(action, index);
							ioItem.code = code;
						}
						ioItem.code = codePrefix + ioItem.code;
						if (StringUtils.isNotEmpty(checkItem.メッセージID)) {
							ioItem.msg_code_ng = checkItem.メッセージID;
						}
						ioItem.description = escapseXml(checkItem.仕様説明);
						StringBuilder sb = new StringBuilder();
						sb.append(String.format("IF( %s ,\n", actionConditionSb.toString()));
						if (checkItem.仕様説明.contains("<チェック条件>")) {
							String checkCondtion = checkItem.仕様説明
									.substring(checkItem.仕様説明.indexOf("<チェック条件>") + "<チェック条件>".length());
							checkCondtion = checkCondtion.replace("\n", " ");
							sb.append(String.format("      IF((%s),\n", checkCondtion));
						} else if (checkItem.仕様説明.contains("(＜チェック条件＞")) {
							String checkCondtion = checkItem.仕様説明
									.substring(checkItem.仕様説明.indexOf("(＜チェック条件＞") + "(＜チェック条件＞".length());
							checkCondtion = checkCondtion.replace("\n", " ");
							sb.append(String.format("      IF((%s),\n", checkCondtion));
						} else {
							sb.append(String.format("      IF((%s),\n", "XXXXXXXX"));
						}
						sb.append(String.format("		   @FALSE,\n"));
						sb.append(String.format("		   @TRUE),\n"));
						sb.append(String.format("@TRUE)"));
						ioItem.condition = escapseXml(sb.toString());
						ioItem.is_disable = "true";
						ioItemList.add(ioItem);
					});
				}
			});
		}
		replaceMap.put("ioItemList", ioItemList);
		return replaceMap;
	}

	private String getActionIoCode(final String name, final String label) {
		String[] texts = new String[] { name, label };
		for (String text : texts) {
			if (StringUtils.isEmpty(text)) {
				continue;
			}
			for (String[] actionInfo : ACTION_ARRAY) {
				if (text.equals(actionInfo[0])) {
					return (String) actionInfo[1];
				} else if (actionInfo[0].startsWith("○○")) {
					if (text.endsWith(actionInfo[0].substring(2))) {
						return (String) actionInfo[1];
					}
				} else if (actionInfo[0].endsWith("○○")) {
					if (text.startsWith(actionInfo[0].substring(0, actionInfo[0].length() - 2))) {
						return (String) actionInfo[1];
					}
				}
			}
		}
		return null;
	}

	private void setBiko(SR画面項目説明書Bean itemBean, IOItem ioItem) {
		String bikoText = itemBean.備考;
		if (!hasValue(bikoText)) {
			return;
		}
		String[] bikoList = bikoText.split("\n");
		for (String biko : bikoList) {
			if (biko.startsWith("fieldType")) {
				biko = biko.replace("：", ":");
				if (biko.indexOf(":") != -1) {
					String value = biko.substring(biko.indexOf(":") + 1);
					if (ioItem.io_item_prop_list == null) {
						ioItem.io_item_prop_list = new ArrayList<ItemProp>();
					}
					ioItem.io_item_prop_list.add(new ItemProp("fieldType", escapseXml(value)));
				}
			} else if (biko.startsWith("defaultFieldStyle")) {
				biko = biko.replace("：", ":");
				if (biko.indexOf(":") != -1) {
					String value = biko.substring(biko.indexOf(":") + 1);
					if (ioItem.io_item_prop_list == null) {
						ioItem.io_item_prop_list = new ArrayList<ItemProp>();
					}
					ioItem.io_item_prop_list.add(new ItemProp("defaultFieldStyle", escapseXml(value)));
				}
			} else if (biko.startsWith("ラベル")) {
				biko = biko.replace("：", ":");
				if (biko.indexOf(":") != -1) {
					String value = biko.substring(biko.indexOf(":") + 1);
					ioItem.label = escapseXml(value);
				}
			}
		}
		ioItem.description = escapseXml(bikoText);
	}
//
//	private String appendToText(String value, String textAppend) {
//		if (StringUtils.isEmpty(textAppend)) {
//			return value;
//		}
//		if (value == null) {
//			value = "";
//		}
//		return value += "\n" + textAppend;
//	}

	private String escapseXml(String xml) {
		return StringEscapeUtils.escapeXml11(xml);
	}

	private static final Pattern CONDTION_PATTERN = Pattern.compile("([^　 =]+) *=");

	private String getCondition(String subLogPrefix, SR画面項目説明書Bean itemBean) {
		String condition = itemBean.表示条件;
		if (!hasValue(condition)) {
			return null;
		}
		return escapseXml(condition);
	}

	private static final Pattern PARAM_PATTERN = Pattern.compile("@\\d+");

	private String getInitValue(String subLogPrefix, SR画面項目説明書Bean itemBean) {
		String initValue = itemBean.初期値;
		if (!hasValue(initValue)) {
			return null;
		}
		if (initValue.startsWith("受取パラメータ")) {
			initValue = initValue.substring("受取パラメータ".length()).replaceAll("　", " ").trim();
		} else if (initValue.startsWith("@") && initValue.contains("パラメータ")) {
			Matcher m = PARAM_PATTERN.matcher(initValue);
			if (m.find()) {
				initValue = m.group();
			}
		}
		return escapseXml(initValue);
	}

	private String getStatement(String subLogPrefix, SR画面項目説明書Bean itemBean) {
		String statement = itemBean.加工式;
		if (!hasValue(statement)) {
			return null;
		}
		Map<String, List<String>> map = parseDefineCell(itemBean, statement);
		if (!map.isEmpty()) {

			List<String> dmList = map.get("DM");
			List<String> conditionList = map.get("条件");
			List<String> valueList = map.get("値");

			if (dmList == null || dmList.isEmpty()) {
				writeWarnLog(subLogPrefix + "項番[{}] 加工式の【DM】が見つかりません", itemBean.項番);
				return "'" + escapseXml(statement) + "'";
			}
			String dm = dmList.get(0);
			FindedTableItem findedTableItem = sqlConvert.findTableItem(dm, null);
			if (!findedTableItem.isFind()) {
				writeErrorLog(subLogPrefix + "項番[{}] 加工式の【DM】が見つかりません:{}", itemBean.項番, dm);
				return "'" + escapseXml(statement) + "'";
			}
			String dmCodeResult = findedTableItem.getTableBean().getTableName();
			String conditionResult = "";
			if (conditionList == null || conditionList.isEmpty()) {
				writeErrorLog(subLogPrefix + "項番[{}] 加工式の【条件】が見つかりません", itemBean.項番);
			} else {
			    StringBuffer sb = new StringBuffer();
				for (String condition : conditionList) {
					condition = condition.replace("＝", "=");
					condition = condition.replace("かつ", " and ");
					condition = condition.replace("　", " ");
					condition = condition.replace("’", "'");
					sb.append(condition).append(" ");
				}
				Matcher m = CONDTION_PATTERN.matcher(sb.toString().trim());
				sb.setLength(0);
				while (m.find()) {
					String filedFullName = m.group(1).trim();
					FieldBean fb = sqlConvert.getFieldFromDBName(findedTableItem.getTableBean().getTableFullName(),
							filedFullName);
					if (fb == null) {
						writeErrorLog(subLogPrefix + "項番[{}] 加工式の【条件】が見つかりません:{}", itemBean.項番, filedFullName);
						continue;
					}
					m.appendReplacement(sb, fb.getFieldName() + "=");
				}
				m.appendTail(sb);
				conditionResult = sb.toString();
			}
			String valueResult = "";
			if (valueList == null || valueList.isEmpty()) {
				writeErrorLog(subLogPrefix + "項番[{}] 加工式の【値】が見つかりません", itemBean.項番);
			} else {
				String value = valueList.get(0);
				FieldBean fb = sqlConvert.getFieldFromDBName(findedTableItem.getTableBean().getTableFullName(), value);
				if (fb == null) {
					writeErrorLog(subLogPrefix + "項番[{}] 加工式の【値】が見つかりません:{}", itemBean.項番, value);
				} else {
					valueResult = fb.getFieldName();
				}
			}
			String newStatement = String.format("NAVAL(%s{%s}.%s, @NULL)", dmCodeResult, conditionResult, valueResult);
			return escapseXml(newStatement);
		}
		if (statement.startsWith("@READONLY") || statement.startsWith("読み取り専用")) {
			return "@READONLY";
		}
		return "'" + escapseXml(statement) + "'";
	}

	private void writeErrorLog(String format, Object... arguments) {
		log.error(logPrefix + format, arguments);
		if (logger != null) {
			logger.error(logPrefix + format, arguments);
		}
	}

	private void writeWarnLog(String format, Object... arguments) {
		log.warn(logPrefix + format, arguments);
		if (logger != null) {
			logger.warn(logPrefix + format, arguments);
		}
	}

	private ChoiceBean getChoiceInfo(String subLogPrefix, SR画面項目説明書Bean itemBean) {
		String selectList = itemBean.選択リスト;
		if (!hasValue(selectList)) {
			return null;
		}
		ChoiceBean choiceInfo = null;
		Map<String, List<String>> map = parseDefineCell(itemBean, itemBean.選択リスト);
		if (map.isEmpty()) {
			if (StringUtils.isNotEmpty(itemBean.選択リスト)) {
				choiceInfo = new ChoiceBean();
				choiceInfo.fixed = escapseXml(itemBean.選択リスト);
			}
			return choiceInfo;
		}
		List<String> fixedList = map.get("固定値");
		if (fixedList != null && !fixedList.isEmpty()) {
			choiceInfo = new ChoiceBean();
			choiceInfo.fixed = fixedList.get(0);
		}
		List<String> dmList = map.get("DM");
		List<String> conditionList = map.get("条件");
		List<String> valueList = map.get("値");
		List<String> labelList = map.get("名称");
		List<String> orderList = map.get("表示順");
		if (dmList == null || dmList.isEmpty()) {
			writeWarnLog(subLogPrefix + "項番[{}] 選択リストの【DM】が見つかりません", itemBean.項番);
			return choiceInfo;
		}
		String dm = dmList.get(0);
		FindedTableItem findedTableItem = sqlConvert.findTableItem(dm, null);
		if (!findedTableItem.isFind()) {
			writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【DM】が見つかりません:{}", itemBean.項番, dm);
			return choiceInfo;
		}
		if (choiceInfo == null) {
			choiceInfo = new ChoiceBean();
		}
		choiceInfo.dmCode = findedTableItem.getTableBean().getTableName();
		if (conditionList == null || conditionList.isEmpty()) {
			writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【条件】が見つかりません", itemBean.項番);
		} else {
		    StringBuffer sb = new StringBuffer();
			for (String condition : conditionList) {
				condition = condition.replace("＝", "=");
				condition = condition.replace("かつ", " and ");
				condition = condition.replace("　", " ");
				condition = condition.replace("’", "'");
				sb.append(condition).append(" ");
			}
			Matcher m = CONDTION_PATTERN.matcher(sb.toString().trim());
			sb.setLength(0);
			while (m.find()) {
				String filedFullName = m.group(1).trim();
				FieldBean fb = sqlConvert.getFieldFromDBName(findedTableItem.getTableBean().getTableFullName(),
						filedFullName);
				if (fb == null) {
					writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【条件】が見つかりません:{}", itemBean.項番, filedFullName);
					continue;
				}
				m.appendReplacement(sb, fb.getFieldName() + "=");
			}
			m.appendTail(sb);
			choiceInfo.condition = sb.toString();
		}
		if (valueList == null || valueList.isEmpty()) {
			writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【値】が見つかりません", itemBean.項番);
		} else {
			String value = valueList.get(0);
			FieldBean fb = sqlConvert.getFieldFromDBName(findedTableItem.getTableBean().getTableFullName(), value);
			if (fb == null) {
				writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【値】が見つかりません:{}", itemBean.項番, value);
			} else {
				choiceInfo.valueDmItemCode = fb.getFieldName();
			}
		}
		if (labelList == null || labelList.isEmpty()) {
			writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【名称】が見つかりません", itemBean.項番);
		} else {
			for (int i = 0; i < labelList.size(); i++) {
				String label = labelList.get(i);
				FieldBean fb = sqlConvert.getFieldFromDBName(findedTableItem.getTableBean().getTableFullName(), label);
				if (fb == null) {
					writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【名称】が見つかりません:{}", itemBean.項番, label);
				} else {
					try {
						BeanUtils.setProperty(choiceInfo, "nameDmItemCode" + (i + 1), fb.getFieldName());
					} catch (Exception e) {
						writeErrorLog(subLogPrefix + "項番[{}] 選択リスのの【名称】設定エラー", itemBean.項番, label, e);
					}
				}
			}
		}

		if (orderList == null || orderList.isEmpty()) {
			writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【表示順】が見つかりません", itemBean.項番);
		} else {
			for (int i = 0; i < orderList.size(); i++) {
				String order = orderList.get(i);
				String orderType = null;
				if (order.contains("昇順")) {
					orderType = "A";
				} else {
					orderType = "D";
				}
				String orderLabel = order.replace("（昇順）", "").replace("（降順）", "").trim();

				FieldBean fb = sqlConvert.getFieldFromDBName(findedTableItem.getTableBean().getTableFullName(),
						orderLabel);
				if (fb == null) {
					writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【表示順】が見つかりません:{}", itemBean.項番, order);
				} else {
					try {
						BeanUtils.setProperty(choiceInfo, "sortDmItemCode" + (i + 1), fb.getFieldName());
					} catch (Exception e) {
						writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【表示順】設定エラー", itemBean.項番, order, e);
					}
					try {
						BeanUtils.setProperty(choiceInfo, "sortType" + (i + 1), orderType);
					} catch (Exception e) {
						writeErrorLog(subLogPrefix + "項番[{}] 選択リストの【表示順】設定エラー", itemBean.項番, order, e);
					}
				}
			}
		}
		return choiceInfo;
	}

	private static final Pattern DEFINE_PATTERN = Pattern.compile("([^\\{]+)\\{([^\\}]+)\\}\\.([^\\. ]+)");

	private Map<String, List<String>> parseDefineCell(SR画面項目説明書Bean itemBean, String defineValue) {
		Map<String, List<String>> map = new LinkedHashMap<String, List<String>>();
		String tmpValue = defineValue;
		tmpValue = tmpValue.replace("｛", "{").replace("｝", "}").replace("．", ".").replace("　", " ").trim();
		Matcher defineMatcher = DEFINE_PATTERN.matcher(tmpValue);
		if (defineMatcher.find()) {
			List<String> tmpList = new ArrayList<String>();
			tmpList.add(defineMatcher.group(1));
			map.put("DM", tmpList);
			tmpList = new ArrayList<String>();
			tmpList.add(defineMatcher.group(2));
			map.put("条件", tmpList);
			tmpList = new ArrayList<String>();
			tmpList.add(defineMatcher.group(3));
			map.put("値", tmpList);
			return map;
		}

		String[] lines = defineValue.trim().split("\n");
		List<String> curList = null;
		for (String line : lines) {
			if (line.startsWith("【")) {
				line = line.substring(1);
				if (line.indexOf("】") == -1) {
					writeErrorLog("項番[{}] 選択リストの解析失敗、「】」が見つかりません:{}", itemBean.項番, defineValue);
					return map;
				}
				String key = line.substring(0, line.indexOf("】"));
				curList = new ArrayList<String>();
				map.put(key, curList);
				line = line.substring(line.indexOf("】") + 1).trim();
			}
			if (curList != null) {
				line = line.trim();
				if (!StringUtils.isEmpty(line)) {
					curList.add(line);
				}
			} else {
//				writeWarnLog("項番[{}] 選択リストの解析失敗:{}", itemBean.項番, defineValue);
				continue;
			}
		}
		return map;
	}

	public void createQueryDM(File wpDir, Map<String, String> queryIdMap) throws IOException {
		List<Map<String, Object>> dmReplaceMaps = getDmReplaceMaps();
		if (dmReplaceMaps != null) {
			for (Map<String, Object> replaceMap : dmReplaceMaps) {
				String queryId = (String) replaceMap.get("dmId");
				if (!queryIdMap.containsKey(queryId)) {
					queryIdMap.put(queryId, xxsjBean.画面ID);
					createTemplate.create(wpDir, replaceMap, "dm");
				} else {
					log.error("データモデルが重複しています。:{}-{},　初回使う画面ID:{}", xxsjBean.画面ID, queryId, queryIdMap.get(queryId));
				}
			}
		}
	}

	public void createALL(File wpDir, String parentDir, Map<String, String> queryIdMap) throws IOException {
		createIO(wpDir, parentDir);
		createExport(wpDir, parentDir);
		createQueryDM(wpDir, queryIdMap);
	}

	public void createIO(File wpDir, String parentDir) throws IOException {
		createTemplate.create(wpDir, getIoReplaceMap(parentDir, "IO"), "io");
	}

	public void createExport(File wpDir, String parentDir) throws IOException {
		if (xxsjBean.list画面項目説明書CSV != null) {
			createTemplate.create(wpDir, getIoReplaceMap(parentDir, "EXPORT"), "io");
		}
	}

	public static void main(String[] args) throws IOException {
		SRSqlConvert sqlConvert = SRUtils.getSqlConvert();
		MorphemHelper HELPER = SRUtils.getMorphemHelper();
		Map<String, String> queryIdMap = new HashMap<String, String>();

		File file = new File("C:\\Users\\bai.chen\\Downloads\\SRTWHB8502_客先コード変換承認一括登録.xlsx");
		File wpDir = new File("./target/wp_out");
		if (wpDir.exists()) {
			FileUtil.cleanDirectory(wpDir);
		}

		try {
			SR詳細設計Reader reader = new SR詳細設計Reader(file.getAbsolutePath(), sqlConvert);
			reader.read();
			List<TableBean> queryTableBeans = reader.getQueryTableBeans();
			if (queryTableBeans != null) {
				sqlConvert.addTableBean(queryTableBeans.toArray(new TableBean[queryTableBeans.size()]));
			}
			SRWprxCreator creator = new SRWprxCreator(reader.getSr詳細設計Bean(), sqlConvert, HELPER);
//			creator.createIO(wpDir, file.getParentFile().getName());
//			createTemplate.create(wpDir, creator.getIoReplaceMap(file.getParentFile().getName()), "io");
			creator.createALL(wpDir, file.getParentFile().getName(), queryIdMap);
//			List<Map<String, Object>> dmReplaceMaps = creator.getDmReplaceMaps();
//			if (dmReplaceMaps != null) {
//				for (Map<String, Object> replaceMap : dmReplaceMaps) {
//					String queryId = (String) replaceMap.get("dmId");
//					if (!queryIdMap.containsKey(queryId)) {
//						queryIdMap.put(queryId, reader.getSr詳細設計Bean().画面ID);
//						createTemplate.create(wpDir, replaceMap, "dm");
//					} else {
//						log.error("データモデルが重複しています。:{}-{},　初回使う画面ID:{}", reader.getSr詳細設計Bean().画面ID, queryId,
//								queryIdMap.get(queryId));
//					}
//				}
//			}
			reader.close();
		} catch (EmptyFileException e) {
			e.printStackTrace();
		}
	}

	public static void main1(String[] args) throws IOException {

		SRSqlConvert sqlConvert = SRUtils.getSqlConvert();
		MorphemHelper HELPER = SRUtils.getMorphemHelper();
//C:\Users\bai.chen\Downloads\SRTWHB8502_客先コード変換承認一括登録.xlsx
		File inputDir = new File("C:\\Users\\bai.chen\\Downloads\\02_販売管理(HB)\\02-1_レンタル");
		File wpDir = new File("./target/wp_out");
		if (wpDir.exists()) {
			FileUtil.cleanDirectory(wpDir);
		}
		List<String> docList = FileUtil.listFileNames(inputDir, new String[] { ".xlsx" });
		Map<String, String> queryIdMap = new HashMap<String, String>();
		for (String fn : docList) {
			if (fn.toLowerCase().contains("/bk/")) {
				continue;
			}
			File file = new File(fn);
			if (file.getName().startsWith("~$")) {
				continue;
			}
//			if (!fn.contains("SRTWHB1101") /* && !fn.contains("SRTWHB0002") */) {
//				continue;
//			}
			try {
				System.out.println(fn);
				SR詳細設計Reader reader = new SR詳細設計Reader(fn, sqlConvert);
				reader.read();
				List<TableBean> queryTableBeans = reader.getQueryTableBeans();
				if (queryTableBeans != null) {
					sqlConvert.addTableBean(queryTableBeans.toArray(new TableBean[queryTableBeans.size()]));
				}
				SRWprxCreator creator = new SRWprxCreator(reader.getSr詳細設計Bean(), sqlConvert, HELPER);
//				creator.createIO(wpDir, file.getParentFile().getName());
//				createTemplate.create(wpDir, creator.getIoReplaceMap(file.getParentFile().getName()), "io");
				creator.createALL(wpDir, file.getParentFile().getName(), queryIdMap);
//				List<Map<String, Object>> dmReplaceMaps = creator.getDmReplaceMaps();
//				if (dmReplaceMaps != null) {
//					for (Map<String, Object> replaceMap : dmReplaceMaps) {
//						String queryId = (String) replaceMap.get("dmId");
//						if (!queryIdMap.containsKey(queryId)) {
//							queryIdMap.put(queryId, reader.getSr詳細設計Bean().画面ID);
//							createTemplate.create(wpDir, replaceMap, "dm");
//						} else {
//							log.error("データモデルが重複しています。:{}-{},　初回使う画面ID:{}", reader.getSr詳細設計Bean().画面ID, queryId,
//									queryIdMap.get(queryId));
//						}
//					}
//				}
				reader.close();
			} catch (EmptyFileException e) {
				continue;
			}
		}
	}

}
