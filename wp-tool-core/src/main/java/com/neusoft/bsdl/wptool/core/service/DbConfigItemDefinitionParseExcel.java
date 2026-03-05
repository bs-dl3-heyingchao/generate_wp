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
import com.neusoft.bsdl.wptool.core.model.DBConfigItemDefinition;
import com.neusoft.bsdl.wptool.core.model.DBConfigSubItemDefinition;

import lombok.extern.slf4j.Slf4j;
/**
 * DB設定項目定義のコンテンツの解析ツール
 */
@Slf4j
public class DbConfigItemDefinitionParseExcel {

	public DBConfigItemDefinition parseSpecSheet(FileSource source, String sheetName) throws Exception {
		// Step 1: 将 InputStream 转为 byte[]，避免流被消费后无法复用
		byte[] excelBytes;
		try (InputStream is = source.getInputStream()) {
			excelBytes = is.readAllBytes();
		}

		// Step 2: 用 POI 读取上部元数据（前4行）
		DBConfigItemDefinition result;
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			result = readHeaderMetadata(sheet);
		}

		// Step 3: 用 EasyExcel 读取下部字段列表（CsvSubLayout）
		List<DBConfigSubItemDefinition> subLayouts = new ArrayList<>();
		AnalysisEventListener<DBConfigSubItemDefinition> listener = new AnalysisEventListener<DBConfigSubItemDefinition>() {
			@Override
			public void invoke(DBConfigSubItemDefinition row, AnalysisContext context) {
				if (row != null && !"項目".equals(row.getItem())) {
					log.debug("Parsed field: item={}, configContent={}", row.getItem(), row.getConfigContent());
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
			EasyExcel.read(bis, DBConfigSubItemDefinition.class, listener).sheet(sheetName).headRowNumber(9).doRead();
		}

		// Step 4: 组装结果
		result.setDetails(subLayouts);
		return result;
	}

	/**
	 * ヘッダ情報取得
	 * 
	 * @param sheet
	 * @return
	 */
	private DBConfigItemDefinition readHeaderMetadata(Sheet sheet) {
		DBConfigItemDefinition layout = new DBConfigItemDefinition();
		// 第6行：データモデル | 操作
		Row row0 = sheet.getRow(CommonConstant.START_POS_INDEX+1);
		if (row0 != null) {
			layout.setDataModel(getCellValue(row0.getCell(6)));
			layout.setOperation(getCellValue(row0.getCell(37)));
		}

		// 第7行：ファイル名規則 | 文字コード | 改行コード
		Row row1 = sheet.getRow(CommonConstant.START_POS_INDEX + 2);
		if (row1 != null) {
			layout.setProcessContent(getCellValue(row1.getCell(6)));
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