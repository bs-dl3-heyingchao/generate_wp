package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.google.common.collect.Lists;
import com.neusoft.bsdl.wptool.core.CommonConstant.SCREEN_ITEM_DESCRIPTION_SHEET;
import com.neusoft.bsdl.wptool.core.enums.ScreenItemDescriptionHeaderEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescription;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescriptionResult;

import lombok.extern.slf4j.Slf4j;

/**
 * 画面項目説明書のコンテンツの解析ツール
 */
@Slf4j
public class ScreenItemDescriptionParseExcel extends AbstractParseTool {
	/**
	 * excel解析
	 * 
	 * @param inputStream
	 * @param sheetName
	 * @return
	 * @throws Exception
	 */
	public List<ScreenItemDescriptionResult> parseSpecSheet(FileSource source, String sheetName,
			List<ExcelParseError> errors) throws Exception {
		// バリデーションチェックを実施する
		try (InputStream validInputStream = source.getInputStream();
				Workbook workbook = WorkbookFactory.create(validInputStream)) {
			Sheet sheet = workbook.getSheet(sheetName);
			// 改版履歴の解析コンテンツのバリエーション
			validateHeaders(sheet, errors);

			if (!CollectionUtils.isEmpty(errors)) {
				return null;
			}
		}
		
		 // 明細情報を解析する
		try (InputStream is2 = source.getInputStream()) {
	        GroupingListener listener = new GroupingListener();
	        EasyExcel.read(is2, ScreenItemDescription.class, listener)
	            .sheet(sheetName)
	            .headRowNumber(SCREEN_ITEM_DESCRIPTION_SHEET.START_POS_DATA_INDEX)
	            .doRead();
	        return listener.getResult();
	    }
	}

	/**
	 * 「画面項目説明書」シートのヘッダー列構造のバリデーションチェック
	 * @param sheet シートオブジェクト
	 * @param errors エラーオブジェクト
	 */
	public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
		Row levle0_headerRow = sheet.getRow(SCREEN_ITEM_DESCRIPTION_SHEET.START_POS_HEADER_INDEX);
		Row levle1_headerRow = sheet.getRow(SCREEN_ITEM_DESCRIPTION_SHEET.START_POS_HEADER_INDEX + 1);

		for (ScreenItemDescriptionHeaderEnum header : ScreenItemDescriptionHeaderEnum.values()) {
			String expectedName = header.getDisplayName();
			int expectedIndex = header.getColumnIndex();
			String actualName = "";
			if (header.getLevel() == 0) {
				actualName = getCellValue(levle0_headerRow, expectedIndex).trim();
			} else {
				actualName = getCellValue(levle1_headerRow, expectedIndex).trim();
			}

			if (!expectedName.equals(actualName)) {
				errors.add(new ExcelParseError(sheet.getSheetName(),
						SCREEN_ITEM_DESCRIPTION_SHEET.START_POS_HEADER_INDEX + 1, expectedIndex,
						MessageService.getMessage("error.format.itemDescription.wrongColumn")));
				break;
			}
		}
	}

	public static class GroupingListener extends AnalysisEventListener<ScreenItemDescription> {
		private String currentGroupName = null;
		private List<ScreenItemDescription> currentItems = new ArrayList<>();
		private final List<ScreenItemDescriptionResult> result = new ArrayList<>();

		@Override
		public void invoke(ScreenItemDescription row, AnalysisContext context) {
			if (row == null)
				return;
			// 項番
			String itemNo = Objects.toString(row.getItemNo(), "").trim();
			// 項目名
			String fieldName = Objects.toString(row.getItemName(), "").trim();

			log.info("Processing row: itemNo='{}', fieldName='{}'", itemNo, fieldName);

			// グループ名称設定
			if (fieldName.isEmpty() && !itemNo.isEmpty()) {
				saveCurrentGroup();
				this.currentGroupName = itemNo;
				this.currentItems = Lists.newArrayList();
			}
			// アイテムの値設定
			else if (!itemNo.isEmpty() && !fieldName.isEmpty()) {
				try {
					Integer.parseInt(itemNo);
				} catch (NumberFormatException e) {
					log.warn("无效编号格式，跳过: {}", itemNo);
					return;
				}
				currentItems.add(row);
			}
		}

		/**
		 * 現時点のグループの値を保存する
		 */
		private void saveCurrentGroup() {
			if (currentGroupName != null && !currentItems.isEmpty()) {
				ScreenItemDescriptionResult group = new ScreenItemDescriptionResult();
				group.setGroupName(currentGroupName);
				// ディープコピー
				group.setItems(new ArrayList<>(currentItems));
				result.add(group);
			}
		}

		@Override
		public void doAfterAllAnalysed(AnalysisContext context) {
			saveCurrentGroup();
		}

		public List<ScreenItemDescriptionResult> getResult() {
			return new ArrayList<>(result);
		}
	}
}