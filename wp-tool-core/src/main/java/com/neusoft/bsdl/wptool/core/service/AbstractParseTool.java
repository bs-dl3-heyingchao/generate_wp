package com.neusoft.bsdl.wptool.core.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 * Excelファイルの解析処理で共通利用されるユーティリティメソッドを提供する抽象基底クラス。
 * 
 * <p>主にセル値の安全な取得など、POI（Apache POI）操作における定型処理をカプセル化します。
 */
public abstract class AbstractParseTool {

	/**
	 * 指定された行（{@link Row}）と列インデックスから、セルの値を文字列として安全に取得します。
	 * 
	 * <p>以下の点を考慮して堅牢に実装されています：
	 * <ul>
	 *   <li>{@code row} または {@code cell} が {@code null} の場合は空文字列を返却</li>
	 *   <li>数値セルの場合、整数値（例: 10.0）は小数点以下を削除して {@code "10"} のように整形</li>
	 *   <li>数式セル（FORMULA）はキャッシュされた結果を取得。評価エラー時は {@code "[FORMULA]"} を返却</li>
	 *   <li>文字列セルの前後空白は自動でトリム</li>
	 * </ul>
	 * 
	 * @param row          対象のExcel行オブジェクト（null可）
	 * @param columnIndex  取得対象の列インデックス（0起点）
	 * @return セルの内容を文字列化したもの。取得不能な場合は空文字列（""）
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
			// 数値の場合、整数なら .0 を削除（例: 5.0 → "5"）
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
				// 数式のキャッシュ結果を取得
				if (cell.getCachedFormulaResultType() == org.apache.poi.ss.usermodel.CellType.STRING) {
					return cell.getStringCellValue();
				} else {
					return String.valueOf(cell.getNumericCellValue());
				}
			} catch (Exception e) {
				// 数式の評価に失敗した場合（例: #REF!, #VALUE! など）
				return "[FORMULA]";
			}
		default:
			return "";
		}
	}
}