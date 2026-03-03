package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.StringUtils;
import com.neusoft.bsdl.wptool.core.CommonConstant;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescription;

import lombok.extern.slf4j.Slf4j;

/**
 * 画面項目説明書のコンテンツの解析ツール
 */
@Slf4j
public class GamenItemExplainParseExcel {

	public List<ScreenItemDescription> parseSpecSheet(FileSource source, String sheetName) throws Exception {
		try (InputStream inputStream = source.getInputStream()) {
			List<ScreenItemDescription> result = new ArrayList<>();

			AnalysisEventListener<ScreenItemDescription> listener = new AnalysisEventListener<ScreenItemDescription>() {
				@Override
				public void invoke(ScreenItemDescription row, AnalysisContext context) {
					log.info("row:" + row.toString());
					// 項番は空白でない場合、該当行が有効とする(項番タイトル行をスキップする)
					if (row != null && !StringUtils.isEmpty(row.getItemNo())
							&& !CommonConstant.SKIP_HEADER.equals(row.getItemNo())) {
						result.add(row);
					}
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
				}
			};
			
			EasyExcel.read(inputStream, ScreenItemDescription.class, listener).sheet(sheetName).headRowNumber(CommonConstant.START_POS_INDEX) 
					.doRead();

			return result;
		}
	}
}
