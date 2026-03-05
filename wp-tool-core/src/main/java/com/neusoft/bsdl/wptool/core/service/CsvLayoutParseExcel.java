package com.neusoft.bsdl.wptool.core.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.neusoft.bsdl.wptool.core.CommonConstant;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.CsvLayout;
import com.neusoft.bsdl.wptool.core.model.CsvSubLayout;

import lombok.extern.slf4j.Slf4j;

/**
 * CSVレイアウトのコンテンツの解析ツール
 */
@Slf4j
public class CsvLayoutParseExcel {

	public CsvLayout parseSpecSheet(FileSource source, String sheetName) throws Exception {
		// Step 1: 将 InputStream 转为 byte[]，避免流被消费后无法复用
		byte[] excelBytes;
		try (InputStream is = source.getInputStream()) {
			excelBytes = is.readAllBytes();
		}

		// Step 2: 用 POI 读取上部元数据（前4行）
		CsvLayout result;
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			result = readHeaderMetadata(sheet);
		}

		// Step 3: 用 EasyExcel 读取下部字段列表（CsvSubLayout）
		List<CsvSubLayout> subLayouts = new ArrayList<>();
		AnalysisEventListener<CsvSubLayout> listener = new AnalysisEventListener<CsvSubLayout>() {
			@Override
			public void invoke(CsvSubLayout row, AnalysisContext context) {
				if (row != null && !CommonConstant.SKIP_HEADER.equals(row.getItemNo())) {
					log.debug("Parsed field: itemNo={}, fieldName={}", row.getItemNo(), row.getFiledName());
					subLayouts.add(row);
				}
			}

			@Override
			public void doAfterAllAnalysed(AnalysisContext context) {
				// nothing
			}
		};

		// ✅ 正确用法：传入新的 ByteArrayInputStream
		try (InputStream bis = new ByteArrayInputStream(excelBytes)) {
			EasyExcel.read(bis, CsvSubLayout.class, listener).sheet(sheetName).headRowNumber(9).doRead();
		}

		// Step 4: 组装结果
		result.setCsvSubLayouts(subLayouts);
		return result;
	}

	/**
	 * ヘッダ情報取得
	 * 
	 * @param sheet
	 * @return
	 */
	private CsvLayout readHeaderMetadata(Sheet sheet) {
		CsvLayout layout = new CsvLayout();

		// 第5行：機能名称 | ファイルID | ファイル名 | 入出力種別 | ファイル形式
		Row row0 = sheet.getRow(CommonConstant.START_POS_INDEX);
		if (row0 != null) {
			layout.setFunctionName(getCellValue(row0.getCell(6)));
			layout.setFileId(getCellValue(row0.getCell(20)));
			layout.setFileName(getCellValue(row0.getCell(34)));
			layout.setInputOutputType(getCellValue(row0.getCell(46)));
			layout.setFileFormat(getCellValue(row0.getCell(57)));
		}

		// 第6行：ファイル名規則 | 文字コード | 改行コード
		Row row1 = sheet.getRow(CommonConstant.START_POS_INDEX + 1);
		if (row1 != null) {
			layout.setFileNamingRule(getCellValue(row1.getCell(6)));
			layout.setCharacterEncoding(getCellValue(row1.getCell(46)));
			layout.setLineEncoding(getCellValue(row1.getCell(57)));
		}
		//特記事項
		Row row2 = sheet.getRow(CommonConstant.START_POS_INDEX + 2);
		if (row2 != null) {
			layout.setSpecialNotes(getCellValue(row2.getCell(6)));
		}
		return layout;
	}
	
	/**
	 * セール値取得
	 * @param cell
	 * @return
	 */
	private String getCellValue(Cell cell) {
		if (cell == null)
			return "";
		switch (cell.getCellType()) {
		case STRING:
			return cell.getStringCellValue().trim();
		case NUMERIC:
			return String.valueOf((int) cell.getNumericCellValue());
		default:
			return "";
		}
	}
}