package com.neusoft.bsdl.wptool.core.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.StringUtils;
import com.google.common.collect.Lists;
import com.neusoft.bsdl.wptool.core.CommonConstant;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenValidation;
import com.neusoft.bsdl.wptool.core.model.ScreenValidationAction;

import lombok.extern.slf4j.Slf4j;

/**
 * 画面チェック仕様書のExcelを解析するサービス
 */
@Slf4j
public class ScreenValidationParseExcel {

	// チェックアクションエリアのスタートインデックス
	private static final int ACTION_START_INDEX = 50;
	// チェックアクションエリアの最大の列数
	private static final int ACTION_COLUMN_COUNT = 12;
	
    /**
     * 解析チェック仕様シート
     */
    public List<ScreenValidation> parseSpecSheet(FileSource source, String sheetName) throws Exception {
        // Step 1: 读取整个文件到内存
        byte[] excelBytes;
        try (InputStream is = source.getInputStream()) {
            excelBytes = is.readAllBytes();
        }

        // Step 2: 用 POI 读取表头（第4行），获取チェックアクション列名
        List<String> actionColumnNames = readActionColumnNames(excelBytes, sheetName);
        log.info("actionColumnNames:{}", actionColumnNames);
        
        // Step 3: 用 EasyExcel 读取数据行（Map<Integer, String>）
        List<ScreenValidation> result = new ArrayList<>();

        try (InputStream bis = new ByteArrayInputStream(excelBytes)) {
            EasyExcel.read(bis, new AnalysisEventListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> rowMap, AnalysisContext context) {
                    String itemNo = rowMap.get(0);
                    if (!isValidItemNo(itemNo)) {
                        return;
                    }

                    ScreenValidation v = buildScreenValidation(rowMap, actionColumnNames);
                    result.add(v);
                    log.debug("Parsed: itemNo={}, validationName={}", v.getItemNo(), v.getValidationName());
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("画面チェック仕様解析完了: {}件", result.size());
                }
            })
            .sheet(sheetName)
            .headRowNumber(CommonConstant.START_POS_INDEX+1)
            .doRead();
        }

        return result;
    }

    /**
     * 用 POI 读取第4行（HEADER_ROW_INDEX）中 index=50~61 的列名
     */
    private List<String> readActionColumnNames(byte[] excelBytes, String sheetName) throws Exception {
        List<String> names = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet(sheetName);
            Row headerRow = sheet.getRow(CommonConstant.START_POS_INDEX+1); 
            int columnIndex = ACTION_START_INDEX;
			for (int i = 0; i < ACTION_COLUMN_COUNT; i++) {
				Cell cell = headerRow.getCell(columnIndex);
				if (cell != null) {
					names.add(cell.toString().trim());
				}
				columnIndex = columnIndex + 2;
			}
        }
        return names;
    }

    /**
     * 从 rowMap 构建 ScreenValidation 对象
     */
    private ScreenValidation buildScreenValidation(Map<Integer, String> rowMap, List<String> actionNames) {
    	ScreenValidation screenValidation = new ScreenValidation();
		// 項番
		screenValidation.setItemNo(rowMap.get(0));
		// 項目名
		screenValidation.setItemName(rowMap.get(1));
		// チェック名
		screenValidation.setValidationName(rowMap.get(7));
		// 種別
		screenValidation.setType(rowMap.get(18));
		// チェック仕様
		screenValidation.setValidationRule(rowMap.get(22));
		// メッセージID
		screenValidation.setMassageId(rowMap.get(74));
		// メッセージ内容
		screenValidation.setMessageContent(rowMap.get(79));
		// パラメータ: {1}
		screenValidation.setParameter1(rowMap.get(105));
		// パラメータ: {2}
		screenValidation.setParameter2(rowMap.get(111));
		// パラメータ: {3}
		screenValidation.setParameter3(rowMap.get(117));
		// パラメータ: {4}
		screenValidation.setParameter4(rowMap.get(123));
		// パラメータ: {5}
		screenValidation.setParameter5(rowMap.get(129));
		// 備考
		screenValidation.setRemarks(rowMap.get(135));
		// IO,BP,ワーニング
		screenValidation.setBizWarining(rowMap.get(171));
		// 実装メモ
		screenValidation.setCodingMemo(rowMap.get(172));

		// ✅ 构建 validationActions
		List<ScreenValidationAction> actions = Lists.newArrayList();
		int columnIndex = ACTION_START_INDEX;
		for (int i = 0; i < ACTION_COLUMN_COUNT; i++) {
			//タイトルは空白の場合、処理終了
			if(StringUtils.isEmpty(actionNames.get(i))) {
				break;
			}
			String cellValue = rowMap.get(columnIndex);
			ScreenValidationAction flag = new ScreenValidationAction();
			flag.setActionName(actionNames.get(i));
			flag.setHasChecked("〇".equals(cellValue));
			actions.add(flag);
			columnIndex = columnIndex +2;
		}
		screenValidation.setValidationActions(actions);

		return screenValidation;
    }

    /**
     * 判断是否为有效项番（非空且可转为数字）
     */
    private boolean isValidItemNo(String itemNo) {
        if (itemNo == null || itemNo.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(itemNo.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}