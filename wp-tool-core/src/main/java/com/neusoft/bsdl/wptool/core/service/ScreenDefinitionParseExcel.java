package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.neusoft.bsdl.wptool.core.CommonConstant.SCREEN_DEFINITION_SHEET;
import com.neusoft.bsdl.wptool.core.enums.ScreenDefinitionParseSectionEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenDefinition;
import com.neusoft.bsdl.wptool.core.model.ScreenDefinitionPartInOut;
import com.neusoft.bsdl.wptool.core.model.ScreenDefinitionProcessingTarget;
import com.neusoft.bsdl.wptool.core.model.ScreenDefinitionTargetData;

/**
 * 「画面定義書」Excelシートを解析し、{@link ScreenDefinition} モデルに変換するためのツールクラス。
 * 
 * <p>
 * この定義書は以下の主要セクションから構成されます：
 * <ul>
 * <li><b>対象データモデル</b>：A列に「対象データモデル」と記載されたブロック</li>
 * <li><b>処理対象データモデル</b>：A列に「処理対象データモデル」と記載されたブロック（CRUD情報含む）</li>
 * <li><b>対象条件</b>：U列に「対象条件」と記載され、その下のU～BP列に条件文が記述</li>
 * <li><b>入出力タイプ</b>：U列に「入出力タイプ」と記載され、その直下に値が記載</li>
 * <li><b>外部ファイル</b>：AB列に「外部ファイル」と記載され、AB列（項目名）とAG列（値）のペアで定義</li>
 * </ul>
 * 
 * <p>
 * 解析は {@link EasyExcel} を用いて全行を生文字列リストとして読み込み、 セクションタイトルの位置を基に各情報を抽出します。
 */
public class ScreenDefinitionParseExcel {

	/**
	 * 指定されたExcelファイルソースから「画面定義書」シートを解析し、 {@link ScreenDefinition} オブジェクトを構築して返却します。
	 * 
	 * <p>
	 * 処理フロー：
	 * <ol>
	 * <li>EasyExcel で全セルを {@code List<List<String>>} 形式で読み込み</li>
	 * <li>「対象データモデル」「処理対象データモデル」ブロックを A 列のタイトルで特定し解析</li>
	 * <li>「対象条件」「入出力タイプ」を U 列のキーワードで検索し内容を抽出</li>
	 * <li>「外部ファイル」セクションを AB 列で検出し、キー-値マップとして取得</li>
	 * </ol>
	 * 
	 * @param source    解析対象のExcelファイルソース
	 * @param sheetName 解析対象のシート名
	 * @param errors    現在未使用（将来拡張用）。null可
	 * @return 解析結果の {@link ScreenDefinition} オブジェクト
	 * @throws Exception Excelの読み込みまたは解析中に予期せぬ例外が発生した場合
	 */
	public ScreenDefinition parseSpecSheet(FileSource source, String sheetName, List<ExcelParseError> errors)
			throws Exception {
		try (InputStream inputStream = source.getInputStream()) {
			List<List<String>> allRows = new ArrayList<>();
			EasyExcel.read(inputStream, new AnalysisEventListener<Map<Integer, String>>() {
				@Override
				public void invoke(Map<Integer, String> rowMap, AnalysisContext context) {
					if (rowMap == null || rowMap.isEmpty()) {
						allRows.add(new ArrayList<>());
						return;
					}
					// 最大列インデックスを取得し、欠損列を null で埋める
					int maxCol = rowMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
					List<String> row = new ArrayList<>(Math.max(0, maxCol + 1));
					for (int i = 0; i <= maxCol; i++) {
						row.add(rowMap.get(i));
					}
					allRows.add(row);
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
					// 全行読み込み後の後処理（現状不要）
				}
			}).sheet(sheetName).doRead();

			ParseResult result = new ParseResult();

			// 対象データモデル（非処理対象）
			parseBlock(allRows, ScreenDefinitionParseSectionEnum.TARGET_MODEL.getSectionName(), result,
					Boolean.FALSE.booleanValue());

			// 処理対象データモデル（CRUD含む）
			parseBlock(allRows, ScreenDefinitionParseSectionEnum.PROCESS_MODEL.getSectionName(), result,
					Boolean.TRUE.booleanValue());

			// 「対象条件」と「入出力タイプ」の開始行を U 列から探索
			Integer condStartRowNum = null;
			Integer ioTypeStartRowNum = null;

			for (int rowNum = 0; rowNum < allRows.size(); rowNum++) {
				String uCell = StringUtils
						.trim(rowNum < allRows.size() && allRows.get(rowNum).size() > SCREEN_DEFINITION_SHEET.COL_U
								? allRows.get(rowNum).get(SCREEN_DEFINITION_SHEET.COL_U)
								: null);
				if (uCell != null) {
					if (ScreenDefinitionParseSectionEnum.CONDITION.getSectionName().equals(uCell)) {
						condStartRowNum = rowNum;
					} else if (ScreenDefinitionParseSectionEnum.IO_TYPE.getSectionName().equals(uCell)) {
						ioTypeStartRowNum = rowNum;
					}
				}
			}

			// 対象条件の内容を U～BP 列から結合して取得（複数行・複数列対応）
			if (condStartRowNum != null && ioTypeStartRowNum != null && condStartRowNum < ioTypeStartRowNum) {
				StringBuilder cond = new StringBuilder();
				for (int rowNum = condStartRowNum + 1; rowNum < ioTypeStartRowNum; rowNum++) {
					List<String> row = allRows.get(rowNum);
					boolean hasContent = false;
					StringBuilder line = new StringBuilder();

					// U列（COL_U）から BP列（COL_BP）までを対象に結合
					for (int col = SCREEN_DEFINITION_SHEET.COL_U; col <= SCREEN_DEFINITION_SHEET.COL_BP; col++) {
						if (row.size() <= col)
							continue;
						String cell = StringUtils.trim(row.get(col));
						if (StringUtils.isNotEmpty(cell)) {
							if (line.length() > 0) {
								line.append(" "); // セル間はスペース区切り
							}
							line.append(cell);
							hasContent = true;
						}
					}

					if (hasContent) {
						if (cond.length() > 0) {
							cond.append("\n"); // 行間は改行区切り
						}
						cond.append(line);
					}
				}
				result.targetCondition = cond.toString();
			}

			// 入出力タイプの値を取得（U列の次行）
			if (ioTypeStartRowNum != null && ioTypeStartRowNum + 1 < allRows.size()) {
				String ioTypeCell = allRows.get(ioTypeStartRowNum + 1).size() > SCREEN_DEFINITION_SHEET.COL_U
						? allRows.get(ioTypeStartRowNum + 1).get(SCREEN_DEFINITION_SHEET.COL_U)
						: null;
				result.ioType = StringUtils.trim(ioTypeCell);
			}

			// 外部ファイルセクションを AB 列から解析（AB=キー、AG=値）
			for (int i = 0; i < allRows.size(); i++) {
				String externalFilesTitles = StringUtils
						.trim(i < allRows.size() && allRows.get(i).size() > SCREEN_DEFINITION_SHEET.COL_AB
								? allRows.get(i).get(SCREEN_DEFINITION_SHEET.COL_AB)
								: null);
				if (ScreenDefinitionParseSectionEnum.EXTERNAL_FILES.getSectionName().equals(externalFilesTitles)) {
					Map<String, String> ext = new HashMap<>();
					for (int rowNum = i + 1; rowNum < allRows.size(); rowNum++) {
						List<String> row = allRows.get(rowNum);
						String item = StringUtils.trim(row.get(SCREEN_DEFINITION_SHEET.COL_AB));
						String value = StringUtils.trim(row.get(SCREEN_DEFINITION_SHEET.COL_AG));
						if (StringUtils.isEmpty(item)) {
							break;
						}
						ext.put(item, value);
					}
					result.externalFiles = ext;
					break;
				}
			}

			// 部分入出力セクションを AI 列から解析
			for (int i = 0; i < allRows.size(); i++) {
				String externalFilesTitles = StringUtils
						.trim(i < allRows.size() && allRows.get(i).size() > SCREEN_DEFINITION_SHEET.COL_AI
								? allRows.get(i).get(SCREEN_DEFINITION_SHEET.COL_AI)
								: null);
				if (ScreenDefinitionParseSectionEnum.INOUT_PART.getSectionName().equals(externalFilesTitles)) {
					List<ScreenDefinitionPartInOut> parts = new ArrayList<>();
					for (int rowNum = i + 1; rowNum < allRows.size(); rowNum++) {
						List<String> row = allRows.get(rowNum);
						// 部分入出力コード
						String code = StringUtils.trim(row.get(SCREEN_DEFINITION_SHEET.COL_AI));
						if (StringUtils.isEmpty(code))
							break;
						// 部分入出力コード
						String name = StringUtils.trim(row.get(SCREEN_DEFINITION_SHEET.COL_AN));
						// 部分入出力名称
						String operation = StringUtils.trim(row.get(SCREEN_DEFINITION_SHEET.COL_BC));
						// 部分入出力オペレーション
						parts.add(new ScreenDefinitionPartInOut(code, name, operation));
					}
					result.inputParts = parts;
					break;
				}
			}

			// 結果オブジェクトを構築
			ScreenDefinition def = new ScreenDefinition();
			def.setTargetModels(result.targetModels);
			def.setProcessingModels(result.processingModels);
			def.setIoType(result.ioType);
			def.setTargetCondition(result.targetCondition);
			def.setExternalFiles(result.externalFiles != null ? result.externalFiles : new HashMap<>());
			def.setInOutParts(result.inputParts);
			return def;

		} catch (Exception e) {
			e.printStackTrace();
			throw new WPParseExcelException("画面定義書の解析に失敗しました: " + e.getMessage());
		}
	}

	/**
	 * 「対象データモデル」または「処理対象データモデル」ブロックを解析し、 論理名・物理名（およびCRUD情報）を抽出します。
	 * 
	 * <p>
	 * ブロックは A 列に指定されたタイトルで始まり、次のセクションタイトルまたは空行で終了します。
	 * 
	 * @param rows         全行データ（各行は列インデックス→値のリスト）
	 * @param title        セクションタイトル（例: 「対象データモデル」）
	 * @param result       解析結果を格納する内部コンテナ
	 * @param isProcessing {@code true} の場合、「処理対象データモデル」（CRUD含む）として処理
	 */
	private void parseBlock(List<List<String>> rows, String title, ParseResult result, boolean isProcessing) {
		Integer titleStartRowNum = findRowIndexColumnA(rows, title);
		if (titleStartRowNum == null)
			return;

		Integer nextTitleRowNum = findNextSectionStart(rows, titleStartRowNum + 1);
		int end = (nextTitleRowNum != null) ? nextTitleRowNum : rows.size();

		// タイトルの2行下（データ開始行）から解析
		for (int rowNum = titleStartRowNum + 2; rowNum < end; rowNum++) {
			if (rowNum >= rows.size())
				break;
			List<String> row = rows.get(rowNum);
			String logical = StringUtils
					.trim(row.size() > SCREEN_DEFINITION_SHEET.COL_A ? row.get(SCREEN_DEFINITION_SHEET.COL_A) : null);
			if (StringUtils.isEmpty(logical))
				break; // 論理名が空なら終了

			String physical = StringUtils
					.trim(row.size() > SCREEN_DEFINITION_SHEET.COL_J ? row.get(SCREEN_DEFINITION_SHEET.COL_J) : null);

			if (isProcessing) {
				String crud = StringUtils
						.trim(row.size() > SCREEN_DEFINITION_SHEET.COL_CRUD ? row.get(SCREEN_DEFINITION_SHEET.COL_CRUD)
								: null);
				result.processingModels.add(new ScreenDefinitionProcessingTarget(logical, physical, crud));
			} else {
				result.targetModels.add(new ScreenDefinitionTargetData(logical, physical));
			}
		}
	}

	/**
	 * A列（COL_A）において、指定されたタイトル文字列が存在する最初の行番号を返却します。
	 * 
	 * @param rows  全行データ
	 * @param title 検索対象のセクションタイトル
	 * @return 見つかった行番号（0起点）。見つからない場合は {@code null}
	 */
	private Integer findRowIndexColumnA(List<List<String>> rows, String title) {
		for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
			String content = StringUtils.trim(rows.get(rowNum).size() > SCREEN_DEFINITION_SHEET.COL_A
					? rows.get(rowNum).get(SCREEN_DEFINITION_SHEET.COL_A)
					: null);
			if (title.equals(content)) {
				return rowNum;
			}
		}
		return null;
	}

	/**
	 * 指定行以降で、A列に定義済みのセクションタイトルが出現する最初の行番号を返却します。
	 * 
	 * <p>
	 * これにより、現在のブロックの終端を特定できます。
	 * 
	 * @param rows      全行データ
	 * @param fromIndex 検索開始行（0起点）
	 * @return 次のセクションの開始行番号。見つからない場合は {@code null}
	 */
	private Integer findNextSectionStart(List<List<String>> rows, int fromIndex) {
		for (int rowNum = fromIndex; rowNum < rows.size(); rowNum++) {
			String content = StringUtils.trim(rows.get(rowNum).size() > SCREEN_DEFINITION_SHEET.COL_A
					? rows.get(rowNum).get(SCREEN_DEFINITION_SHEET.COL_A)
					: null);
			if (StringUtils.isNoneEmpty(content)
					&& ScreenDefinitionParseSectionEnum.getAllDisplayNames().contains(content)) {
				return rowNum;
			}
		}
		return null;
	}

	/**
	 * 画面定義書の解析結果を一時的に保持する内部ヘルパークラス。
	 * 
	 * <p>
	 * 解析中の各セクションの結果をフィールドに集約し、最終的に {@link ScreenDefinition} にマッピングします。
	 */
	private static class ParseResult {
		List<ScreenDefinitionTargetData> targetModels = new ArrayList<>();
		List<ScreenDefinitionProcessingTarget> processingModels = new ArrayList<>();
		String ioType = null;
		String targetCondition = null;
		Map<String, String> externalFiles = new HashMap<>();
		List<ScreenDefinitionPartInOut> inputParts = new ArrayList<>();
	}
}