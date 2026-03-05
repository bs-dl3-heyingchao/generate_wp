package com.neusoft.bsdl.wptool.core.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 * 解析用の共通クラス
 */
public abstract class AbstractParseTool {
	/**
	 * セルの値を安全に文字列として取得
	 * 
	 * @param row
	 * @param columnIndex
	 * @return
	 */
	protected static String getCellValue(Row row, int columnIndex) {
		if (row == null)
			return "";
		Cell cell = row.getCell(columnIndex);
		if (cell == null)
			return "";

		switch (cell.getCellType()) {
		case STRING:
			return cell.getStringCellValue().trim();
		case NUMERIC:
			// 数値の場合、整数なら .0 を削除
			double val = cell.getNumericCellValue();
			if (val == (long) val) {
				return String.valueOf((long) val);
			} else {
				return String.valueOf(val);
			}
		case BOOLEAN:
			return String.valueOf(cell.getBooleanCellValue());
		case FORMULA:
			try {
				return cell.getCachedFormulaResultType() == org.apache.poi.ss.usermodel.CellType.STRING
						? cell.getStringCellValue()
						: String.valueOf(cell.getNumericCellValue());
			} catch (Exception e) {
				return "[FORMULA]";
			}
		default:
			return "";
		}
	}
}
