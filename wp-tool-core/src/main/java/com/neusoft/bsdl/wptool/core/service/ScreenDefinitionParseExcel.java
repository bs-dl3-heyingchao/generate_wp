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
import com.neusoft.bsdl.wptool.core.model.ScreenDefinitionProcessingTarget;
import com.neusoft.bsdl.wptool.core.model.ScreenDefinitionTargetData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScreenDefinitionParseExcel {

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
					// 修复：正确获取最大列索引
					int maxCol = rowMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
					List<String> row = new ArrayList<>(Math.max(0, maxCol + 1));
					for (int i = 0; i <= maxCol; i++) {
						row.add(rowMap.get(i));
					}
					allRows.add(row);
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
				}
			}).sheet(sheetName).doRead();

			ParseResult result = new ParseResult();
			log.info("allRows.size():{},allRows:{}", allRows.size(), allRows);

			// 対象データモデル
			parseBlock(allRows, ScreenDefinitionParseSectionEnum.TARGET_MODEL.getSectionName(), result,
					Boolean.FALSE.booleanValue());

			// 処理対象データモデル
			parseBlock(allRows, ScreenDefinitionParseSectionEnum.PROCESS_MODEL.getSectionName(), result,
					Boolean.TRUE.booleanValue());

			// 対象条件の開始行
			Integer condStartRowNum = null;
			// 入出力タイプの開始行
			Integer ioTypeStartRowNum = null;

			// 開始行設定
			for (int rowNum = 0; rowNum < allRows.size(); rowNum++) {
				String uCell = StringUtils.trim(allRows.get(rowNum).get(SCREEN_DEFINITION_SHEET.COL_U));
				if (uCell != null) {
					if (ScreenDefinitionParseSectionEnum.CONDITION.getSectionName().equals(uCell)) {
						condStartRowNum = rowNum;
					} else if (ScreenDefinitionParseSectionEnum.IO_TYPE.getSectionName().equals(uCell)) {
						ioTypeStartRowNum = rowNum;
					}
				}
			}

			// 対象条件の内容を読込む
			if (condStartRowNum != null && ioTypeStartRowNum != null && condStartRowNum < ioTypeStartRowNum) {
				StringBuilder cond = new StringBuilder();
				for (int rowNum = condStartRowNum + 1; rowNum < ioTypeStartRowNum; rowNum++) {
					List<String> row = allRows.get(rowNum);
					boolean hasContent = false;
					StringBuilder line = new StringBuilder();

					// ループスコープはU列からBP列まで
					for (int col = SCREEN_DEFINITION_SHEET.COL_U; col <= SCREEN_DEFINITION_SHEET.COL_BP; col++) {
						if (row.size() <= col)
							continue;
						String cell = StringUtils.trim(row.get(col));
						if (StringUtils.isNotEmpty(cell)) {
							if (line.length() > 0) {
								// 前のセルに内容があった場合、スペースで区切る
								line.append(" ");
							}
							line.append(cell);
							hasContent = true;
						}
					}

					if (hasContent) {
						if (cond.length() > 0) {
							// 前の行に内容があった場合、改行で区切る
							cond.append("\n");
						}
						cond.append(line);
					}
				}
				result.targetCondition = cond.toString();
			}

			// 读取入出力タイプ的值（U列下一行）
			if (ioTypeStartRowNum != null && ioTypeStartRowNum + 1 < allRows.size()) {
				result.ioType = StringUtils.trim(allRows.get(ioTypeStartRowNum + 1).get(SCREEN_DEFINITION_SHEET.COL_U));
			}

			// 外部ファイル
			for (int i = 0; i < allRows.size(); i++) {
				String externalFilesTitles = StringUtils.trim(allRows.get(i).get(SCREEN_DEFINITION_SHEET.COL_AB));
				if (ScreenDefinitionParseSectionEnum.EXTERNAL_FILES.getSectionName().equals(externalFilesTitles)) {
					Map<String, String> ext = new HashMap<>();
					for (int rowNum = i + 1; rowNum < allRows.size(); rowNum++) {
						String item = StringUtils.trim(allRows.get(rowNum).get(SCREEN_DEFINITION_SHEET.COL_AB));
						String value = StringUtils.trim(allRows.get(rowNum).get(SCREEN_DEFINITION_SHEET.COL_AG));
						// 空白の場合、ループを飛び出す
						if (StringUtils.isEmpty(item)) {
							break;
						}
						ext.put(item, value);
					}
					result.externalFiles = ext;
					break;
				}
			}

			// 构建结果
			ScreenDefinition def = new ScreenDefinition();
			def.setTargetModels(result.targetModels);
			def.setProcessingModels(result.processingModels);
			def.setIoType(result.ioType);
			def.setTargetCondition(result.targetCondition);
			def.setExternalFiles(result.externalFiles != null ? result.externalFiles : new HashMap<>());
			return def;

		} catch (Exception e) {
			e.printStackTrace();
			throw new WPParseExcelException("画面定義書の解析に失敗しました: " + e.getMessage());
		}
	}

	/**
	 * 解析块内容:対象データモデル|処理対象データモデル
	 * 
	 * @param rows         行対象
	 * @param title        タイトル
	 * @param result       結果対象
	 * @param isProcessing 処理対象データモデルかどうか
	 */
	private void parseBlock(List<List<String>> rows, String title, ParseResult result, boolean isProcessing) {
		Integer titleStartRowNum = findRowIndexColumnA(rows, title);
		if (titleStartRowNum == null)
			return;

		Integer nextTitleRowNum = findNextSectionStart(rows, titleStartRowNum + 1);
		int end = (nextTitleRowNum != null) ? nextTitleRowNum : rows.size();

		for (int rowNum = titleStartRowNum + 2; rowNum < end; rowNum++) {
			String logical = StringUtils.trim(rows.get(rowNum).get(SCREEN_DEFINITION_SHEET.COL_A));
			if (StringUtils.isEmpty(logical))
				break;
			String physical = StringUtils.trim(rows.get(rowNum).get(SCREEN_DEFINITION_SHEET.COL_J));
			if (isProcessing) {
				String crud = StringUtils.trim(rows.get(rowNum).get(SCREEN_DEFINITION_SHEET.COL_CRUD));
				result.processingModels.add(new ScreenDefinitionProcessingTarget(logical, physical, crud));
			} else {
				result.targetModels.add(new ScreenDefinitionTargetData(logical, physical));
			}
		}
	}

	/**
	 * タイトルに対応するA列の行を見つける
	 * 
	 * @param rows  行対象
	 * @param title タイトル
	 * @return 行番号
	 */
	private Integer findRowIndexColumnA(List<List<String>> rows, String title) {
		for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
			if (title.equals(StringUtils.trim(rows.get(rowNum).get(SCREEN_DEFINITION_SHEET.COL_A)))) {
				return rowNum;
			}
		}
		return null;
	}

	/**
	 * 次のセクションの開始行を見つける
	 * 
	 * @param rows      行対象
	 * @param fromIndex 開始行
	 * @return 行番号
	 */
	private Integer findNextSectionStart(List<List<String>> rows, int fromIndex) {
		for (int rowNum = fromIndex; rowNum < rows.size(); rowNum++) {
			String content = StringUtils.trim(rows.get(rowNum).get(SCREEN_DEFINITION_SHEET.COL_A));
			if (StringUtils.isNoneEmpty(content)
					&& ScreenDefinitionParseSectionEnum.getAllDisplayNames().contains(content)) {
				return rowNum;
			}
		}
		return null;
	}

	// 内部结果类
	private static class ParseResult {
		List<ScreenDefinitionTargetData> targetModels = new ArrayList<>();
		List<ScreenDefinitionProcessingTarget> processingModels = new ArrayList<>();
		String ioType = "";
		String targetCondition = "";
		Map<String, String> externalFiles = new HashMap<>();
	}
}