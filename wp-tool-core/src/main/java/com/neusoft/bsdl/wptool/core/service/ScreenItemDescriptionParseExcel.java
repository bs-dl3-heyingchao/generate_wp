package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.google.common.collect.Lists;
import com.neusoft.bsdl.wptool.core.CommonConstant;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescription;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescriptionResult;

import lombok.extern.slf4j.Slf4j;

/**
 * 画面項目説明書のコンテンツの解析ツール
 */
@Slf4j
public class ScreenItemDescriptionParseExcel {
	/**
	 * excel解析
	 * 
	 * @param inputStream
	 * @param sheetName
	 * @return
	 * @throws Exception
	 */
	public List<ScreenItemDescriptionResult> parseSpecSheet(FileSource source, String sheetName) throws Exception {
		try (InputStream inputStream = source.getInputStream()) {
			GroupingListener listener = new GroupingListener();
			//五行目からエクセルを解析する
			EasyExcel.read(inputStream, ScreenItemDescription.class, listener).sheet(sheetName)
					.headRowNumber(CommonConstant.START_POS_INDEX).doRead();

			return listener.getResult();
		}
	}

	/**
	 * 
	 */
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
			String fieldName = Objects.toString(row.getFieldName(), "").trim();

			log.info("Processing row: itemNo='{}', fieldName='{}'", itemNo, fieldName);

			// タイトル行をスキップする
			if (CommonConstant.SKIP_HEADER.equals(itemNo)) {
				return;
			}

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
				//ディープコピー
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