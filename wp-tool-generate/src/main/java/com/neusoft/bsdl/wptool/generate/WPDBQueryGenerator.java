package com.neusoft.bsdl.wptool.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.alibaba.excel.util.StringUtils;
import com.neusoft.bsdl.wptool.core.model.DBQueryEntity;
import com.neusoft.bsdl.wptool.core.model.DBQueryJoinCondition;
import com.neusoft.bsdl.wptool.core.model.DBQueryJoinConditionContents;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;
import com.neusoft.bsdl.wptool.generate.model.DmItem;
import com.neusoft.bsdl.wptool.generate.model.DmProp;

import lombok.extern.slf4j.Slf4j;

/**
 * DBクエリ定義シートからSQL文を生成するジェネレータクラスです。
 * <p>
 * このクラスは、Excelシートに記載された以下の情報を元に、 完全なSELECT文（結合条件・WHERE句・CASE式含む）を構築します。
 * <ul>
 * <li>SELECT対象カラムリスト</li>
 * <li>テーブル結合条件（内部結合／外部結合）</li>
 * <li>WHERE句の条件テキスト</li>
 * <li>CASE-WHEN形式の条件分岐（日本語仕様書形式）</li>
 * </ul>
 */
@Slf4j
public class WPDBQueryGenerator extends WPAbstractGenerator<DBQuerySheetContent> {

	public static final String STR_HAIHUN = "-";

	public static final String STR_HANKAKU_SPACE = " ";

	public static final String STR_CTRL = "\n";

	public static final String STR_COMMA = ",";

	public static final String STR_DOT = ".";

	public static final String STR_TAB = "	";

	public static final String STR_NULL = "NULL";

	/**
	 * 日本語条件分岐の各行（例: 「・03：ヒューテックTYPE A の場合：...」）を解析する正規表現です。
	 * <p>
	 * パターンの意味:
	 * <ul>
	 * <li>{@code ・} : 行頭の中黒（箇条書き開始）</li>
	 * <li>{@code (\d+)} : 条件値（数字のみ。例: 03, 18）</li>
	 * <li>{@code [^\n]*?の場合：} : 「の場合：」までの任意の説明文（非貪欲マッチ）</li>
	 * <li>{@code \s*(.+)} : 「場合：」後のSQL式（例: 汎用マスタ_XXX.汎用名 または NULL）</li>
	 * </ul>
	 */
	private static final Pattern LINE_PATTERN = Pattern.compile("・(\\d+)[^\\n]*?の場合：\\s*(.+)");

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
		String sql = createDbQuery(excelContent);
		if (!StringUtils.isEmpty(sql)) {
			String result = context.getSqlConverter().convert(sql);
			log.info("result:{}", result);
			dmPropList.add(new DmProp("dbQuery", escapseXml(sql), "false"));
			replaceMap.put("dmPropList", dmPropList);
		}

		return replaceMap;
	}

	/**
	 * Excelシートから読み取った情報をもとに、完全なSELECT SQL文を構築します。
	 * <p>
	 * 処理内容:
	 * <ol>
	 * <li>SELECT句: 各カラムを処理。AL列にCASE-WHEN条件があれば変換</li>
	 * <li>FROM～JOIN句: 結合条件リストに基づき、INNER/LEFT JOINを生成</li>
	 * <li>WHERE句: 「where」以降のテキストを抽出して付与</li>
	 * </ol>
	 */
	private String createDbQuery(DBQuerySheetContent excelContent) {
		StringBuilder queryBuilder = new StringBuilder();
		String errorMsg = null;
		DBQueryJoinCondition joinCondition = excelContent.getJoinCondition();
		if (ObjectUtils.isEmpty(joinCondition)) {
			errorMsg = String.format("[%s]シート:DBQuery定義書の結合条件の備考が記載なしです、ご確認ください。", excelContent.getSheetName());
			writeErrorLog(errorMsg);
			return null;
		}
		// dbQuery：検索条件
		String queryCondition = excelContent.getQueryCondition();
		if (StringUtils.isEmpty(queryCondition)) {
			errorMsg = String.format("[%s]シート:DBQuery定義書のdbQuery：検索条件が記載なしです、ご確認ください。", excelContent.getSheetName());
			writeErrorLog(errorMsg);
			return null;
		}
		// WHEREのコンディションを洗い出す
		String whereConditon = extractWhereClause(queryCondition);
		// SELECT文の項目のビルダー
		StringBuilder querySelectBuilder = new StringBuilder();
		// 結合条件のビルダー
		StringBuilder queryConditionBuilder = new StringBuilder();
		List<DBQueryJoinConditionContents> normaljoinConditions = joinCondition.getNormaljoinConditions();
		if (!joinCondition.isUnionAllCase()) {
			List<DBQueryEntity> queryEntities = excelContent.getQueryEntities();
			for (int i = 0; i < queryEntities.size(); i++) {
				String caseWhenConditon = queryEntities.get(i).getCaseWhenCondition();
				// AL列の記載が存在しない場合、AK列の記載をSELECTの項目として作成する
				if (StringUtils.isEmpty(caseWhenConditon)) {
					querySelectBuilder.append(STR_TAB + queryEntities.get(i).getResourceTableName() + STR_DOT
							+ queryEntities.get(i).getResourceColumnName());
					if (i < queryEntities.size() - 1) {
						querySelectBuilder.append(STR_COMMA + STR_CTRL);
					}
				} else {
					// AL列の記載が存在する場合、CASE-WHEN条件を作成する
					String parsedCaseWhenConditon = convertToCaseWhen(excelContent.getSheetName(),
							queryEntities.get(i).getItemNo(), caseWhenConditon);
					if (!StringUtils.isEmpty(parsedCaseWhenConditon)) {
						querySelectBuilder.append(parsedCaseWhenConditon).append(STR_CTRL).append(STR_TAB)
								.append("END AS ").append(queryEntities.get(i).getLogicalName())
								.append(STR_COMMA + STR_CTRL);
					} else {
						querySelectBuilder.append("/*ERROR⇒解析できないCASE-WHEN：").append(caseWhenConditon).append("*/")
								.append(STR_COMMA + STR_CTRL);
					}
				}
			}
			// 結合条件エリアが存在する場合
			if (!CollectionUtils.isEmpty(normaljoinConditions)) {
				// 結合条件を作成する
				queryConditionBuilder.append(normaljoinConditions.get(0).getTableName());
				for (int i = 1; i < normaljoinConditions.size(); i++) {
					DBQueryJoinConditionContents current = normaljoinConditions.get(i);
					String joinType = logicalNameToPhicalName(current.getMethod());
					String tableName = current.getTableName();
					String alias = current.getAlias();
					String condition = current.getCondition();

					queryConditionBuilder.append(STR_CTRL).append(joinType).append(STR_HANKAKU_SPACE);

					if (!STR_HAIHUN.equals(alias)) {
						queryConditionBuilder.append(tableName).append(STR_HANKAKU_SPACE).append(alias);
					} else {
						queryConditionBuilder.append(tableName);
					}

					queryConditionBuilder.append(STR_CTRL).append(STR_TAB).append("ON ").append(condition);
				}
				queryBuilder.append("SELECT " + STR_CTRL + querySelectBuilder.toString() + STR_CTRL + "FROM "
						+ queryConditionBuilder.toString() + STR_CTRL + "WHERE" + STR_CTRL + whereConditon);
			} else {
				errorMsg = String.format("[%s]シート:DBQuery定義書の備考の結合条件が記載なしです、ご確認ください。", excelContent.getSheetName());
				writeErrorLog(errorMsg);
				return null;
			}
		}
		return queryBuilder.toString();
	}

	/**
	 * 日本語で記述された条件分岐ブロックを、SQLのCASE WHEN式に変換します。
	 * <p>
	 * 【対応する入力形式】
	 * 
	 * <pre>
	 * 会員管理会社管理マスタ.会員会社CD　が
	 * ・03：ヒューテックTYPE A　の場合：　　　汎用マスタ_会員会社タイプ_CK191.汎用名
	 * ・18：ヒューテックTYPE S　の場合：　　　汎用マスタ_会員会社タイプ_CK198.汎用名
	 * ・08：大都販売　の場合：　　　　　　　　汎用マスタ_会員会社タイプ_CK062.汎用名
	 * ・07：アイゲート(旧 アイ電子) の場合：　汎用マスタ_会員会社タイプ_CK063.汎用名
	 * ・上記以外の場合：　　　　　　　　　　　NULL
	 * </pre>
	 *
	 * 【注意事項】
	 * <ul>
	 * <li>条件値は<strong>数字のみ</strong>（\d+）をサポート。アルファベット混在は非対応</li>
	 * <li>「の場合：」のコロンは<strong>全角</strong>である必要があります</li>
	 * <li>「上記以外の場合」行は最後に1回のみ出現することを想定</li>
	 * </ul>
	 *
	 * @param sheetName        シート名称
	 * @param itemNo           項番（エラーログ用）
	 * @param caseWhenConditon 複数行からなる日本語条件分岐テキスト
	 * @return 変換されたCASE WHEN式（例: "CASE\n\tWHEN XXX = 03 THEN YYY\n\tELSE
	 *         NULL"）。解析失敗時は空文字列
	 */
	public String convertToCaseWhen(String sheetName, String itemNo, String caseWhenConditon) {
		String[] lines = caseWhenConditon.split(STR_CTRL);
		if (lines.length < 2) {
			exportWarningCondition(sheetName, itemNo, caseWhenConditon);
			return "";
		}

		String firstLine = lines[0].trim();
		if (!firstLine.endsWith("が")) {
			exportWarningCondition(sheetName, itemNo, caseWhenConditon);
			return "";
		}
		String columnName = firstLine.substring(0, firstLine.length() - 1).trim();

		StringBuilder caseBuilder = new StringBuilder();
		caseBuilder.append(STR_TAB).append("CASE" + STR_CTRL);

		String elseValue = STR_NULL;

		for (int i = 1; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.isEmpty())
				continue;

			if (line.startsWith("・上記以外の場合：")) {
				String elsePart = line.substring("・上記以外の場合：".length()).trim();
				elseValue = STR_NULL.equalsIgnoreCase(elsePart) ? STR_NULL : elsePart;
				break;
			}

			Matcher m = LINE_PATTERN.matcher(line);
			if (m.matches()) {
				String code = m.group(1);
				String thenExpr = m.group(2).trim();
				if (STR_NULL.equalsIgnoreCase(thenExpr)) {
					thenExpr = STR_NULL;
				}

				caseBuilder.append(STR_TAB).append(STR_TAB).append("WHEN ").append(columnName).append(" = ")
						.append(code).append(" THEN ").append(thenExpr).append(STR_CTRL);
			} else {
				exportWarningCondition(sheetName, itemNo, caseWhenConditon);
				break;
			}
		}

		caseBuilder.append(STR_TAB).append(STR_TAB).append("ELSE ").append(elseValue);
		return caseBuilder.toString();
	}

	/**
	 * 日本語の結合方式（「内部結合」「外部結合」）をSQLのJOINキーワードに変換します。
	 *
	 * @param method 「内部結合」または「外部結合」
	 * @return 「INNER JOIN」または「LEFT OUTER JOIN」。未対応値の場合は "-"
	 */
	private String logicalNameToPhicalName(String method) {
		if ("内部結合".equals(method)) {
			return "INNER JOIN";
		} else if ("外部結合".equals(method)) {
			return "LEFT OUTER JOIN";
		} else {
			return STR_HAIHUN;
		}
	}

	/**
	 * WHERE句のテキストから「where」キーワード以降の実際の条件部分を抽出します。
	 * <p>
	 * 入力例:
	 * 
	 * <pre>
	 * select ...
	 * from ...
	 * where
	 * 1 = 1
	 * AND XXX = YYY
	 * </pre>
	 * 
	 * → 出力: "1 = 1\nAND XXX = YYY"
	 *
	 * @param input 全SQLテキストまたはWHERE句を含むテキスト
	 * @return 抽出されたWHERE条件（whereキーワードを含まない）
	 */
	public static String extractWhereClause(String input) {
		if (input == null)
			return "";

		String lower = input.toLowerCase();
		int whereLowIdx = lower.indexOf("where");
		int whereUpperIdx = lower.indexOf("WHERE");
		if (whereLowIdx == -1 && whereUpperIdx == -1) {
			return "";
		}

		String rest = null;
		if (whereLowIdx > -1) {
			rest = input.substring(whereLowIdx + 5).trim();
		} else if (whereUpperIdx > -1) {
			rest = input.substring(whereUpperIdx + 5).trim();
		}

		String[] lines = rest.split(STR_CTRL);
		StringBuilder result = new StringBuilder();
		boolean foundNonEmpty = false;

		for (String line : lines) {
			String trimmed = line.trim();
			if (!foundNonEmpty) {
				if (!trimmed.isEmpty()) {
					foundNonEmpty = true;
					result.append(STR_TAB + trimmed);
				}
			} else {
				result.append(STR_CTRL).append(STR_TAB + trimmed);
			}
		}

		return result.toString();
	}

	/**
	 * CASE-WHEN条件のパースエラーをログに出力します。
	 *
	 * @param sheetName        シート名称
	 * @param itemNo           項番
	 * @param caseWhenConditon 元の条件テキスト
	 */
	private void exportWarningCondition(String sheetName, String itemNo, String caseWhenConditon) {
		String logSubPrefix = String.format("[%s]シート:項番[%s]、AL列のコンディション[%s]", sheetName, itemNo, caseWhenConditon);
		writeWarnLog("コンディションの形式が不正です:{}", logSubPrefix);
	}
}