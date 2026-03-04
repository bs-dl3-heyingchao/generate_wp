package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.StringUtils;
import com.neusoft.bsdl.wptool.core.CommonConstant;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenFuncSpecification;

/**
 * 画面機能定義書のコンテンツの解析ツール
 */
public class ScreenFuncSpecificationParseExcel {

	public List<ScreenFuncSpecification> parseSpecSheet(FileSource source, String sheetName) throws Exception {
		try (InputStream inputStream = source.getInputStream()) {
			List<ScreenFuncSpecification> result = Lists.newArrayList();

			AnalysisEventListener<ScreenFuncSpecification> listener = new AnalysisEventListener<ScreenFuncSpecification>() {
				@Override
				public void invoke(ScreenFuncSpecification row, AnalysisContext context) {
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
			
			EasyExcel.read(inputStream, ScreenFuncSpecification.class, listener).sheet(sheetName).headRowNumber(CommonConstant.START_POS_INDEX) 
					.doRead();

			return result;
		}
	}
}
