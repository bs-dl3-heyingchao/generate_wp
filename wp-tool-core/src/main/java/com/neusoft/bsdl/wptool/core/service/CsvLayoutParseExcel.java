package com.neusoft.bsdl.wptool.core.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.neusoft.bsdl.wptool.core.CommonConstant.CSV_LAYOUT_SHEET;
import com.neusoft.bsdl.wptool.core.enums.CsvLayoutDetailEnum;
import com.neusoft.bsdl.wptool.core.enums.CsvLayoutEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.CsvLayout;
import com.neusoft.bsdl.wptool.core.model.CsvSubLayout;

/**
 * 「CSVレイアウト定義書」Excelシートを解析し、{@link CsvLayout} モデルに変換するためのツールクラス。
 * 
 * <p>以下の3つの主要処理を実行します：
 * <ol>
 *   <li>ヘッダ領域（3行）と明細ヘッダ領域（2行）の構造バリデーション</li>
 *   <li>ヘッダメタデータ（機能名、ファイルID、エンコーディングなど）の抽出</li>
 *   <li>明細データ（各カラム定義）の {@link EasyExcel} を用いた効率的な読み込み</li>
 * </ol>
 */
public class CsvLayoutParseExcel extends AbstractParseTool {

	/**
	 * 指定されたExcelファイルソースから「CSVレイアウト定義書」シートを解析し、
	 * 構造化された {@link CsvLayout} オブジェクトを返却します。
	 * 
	 * <p>処理フロー：
	 * <ol>
	 *   <li>Excel全体をバイト配列として読み込み</li>
	 *   <li>ヘッダおよび明細ヘッダの構造をバリデーション</li>
	 *   <li>ヘッダメタデータをApache POIで手動解析</li>
	 *   <li>明細データをAlibaba EasyExcelライブラリで自動マッピング</li>
	 * </ol>
	 * 
	 * @param source     解析対象のExcelファイルソース
	 * @param sheetName  解析対象のシート名
	 * @param errors     バリデーションエラーを格納するリスト（null不可）
	 * @return 解析結果の {@link CsvLayout} オブジェクト。エラー発生時は {@code null}
	 * @throws Exception Excelの読み込みまたは解析中に予期せぬ例外が発生した場合
	 */
	public CsvLayout parseSpecSheet(FileSource source, String sheetName, List<ExcelParseError> errors)
			throws Exception {
		// エクセルファイルを読込む
		byte[] excelBytes;
		try (InputStream is = source.getInputStream()) {
			excelBytes = is.readAllBytes();
		}

		// バリデーションチェックを実施する
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			validateHeaders(sheet, errors);
			if (!CollectionUtils.isEmpty(errors)) {
				return null;
			}
		}

		// ヘッダ情報を解析する
		CsvLayout result;
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
			Sheet sheet = workbook.getSheet(sheetName);
			result = readHeaderMetadata(sheet);
		}

		// 明細情報を解析する（EasyExcelによる自動マッピング）
		List<CsvSubLayout> subLayouts = new ArrayList<>();
		AnalysisEventListener<CsvSubLayout> listener = new AnalysisEventListener<CsvSubLayout>() {
			@Override
			public void invoke(CsvSubLayout row, AnalysisContext context) {
				if (row != null) {
					subLayouts.add(row);
				}
			}

			@Override
			public void doAfterAllAnalysed(AnalysisContext context) {
				// 全行読み込み後の後処理（現状不要）
			}
		};

		try (InputStream bis = new ByteArrayInputStream(excelBytes)) {
			EasyExcel.read(bis, CsvSubLayout.class, listener).sheet(sheetName)
					.headRowNumber(CSV_LAYOUT_SHEET.START_POS_DATA_INDEX).doRead();
		}

		result.setCsvSubLayouts(subLayouts);
		return result;
	}

	/**
	 * 「CSVレイアウト」シートのヘッダ領域（3行）および明細ヘッダ領域（2行）の構造が、
	 * 定義書仕様に準拠しているかを検証します。
	 * 
	 * <p>不一致が検出された場合、{@code errors} リストにエラー情報を追加します。
	 * 
	 * <ul>
	 *   <li><b>ヘッダ領域</b>：{@link CsvLayoutEnum} に基づき、3階層（level 0～2）のヘッダを検証</li>
	 *   <li><b>明細ヘッダ領域</b>：{@link CsvLayoutDetailEnum} に基づき、2階層（level 0～1）を検証</li>
	 * </ul>
	 * 
	 * @param sheet  検証対象のExcelシート
	 * @param errors 検証エラーを格納するリスト（null不可）
	 */
	public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
		Row level0Header = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX);
		Row level1Header = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX + 1);
		Row level2Header = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX + 2);

		// 主ヘッダ（3行構成）
		for (CsvLayoutEnum header : CsvLayoutEnum.values()) {
			String expected = header.getDisplayName();
			int colIndex = header.getColumnIndex();
			String actual = "";
			Row targetRow = switch (header.getLevel()) {
			case 0 -> level0Header;
			case 1 -> level1Header;
			case 2 -> level2Header;
			default -> null;
			};
			if (targetRow != null) {
				actual = getCellValue(targetRow, colIndex).trim();
			}
			if (!expected.equals(actual)) {
				errors.add(new ExcelParseError(sheet.getSheetName(),
						CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX + header.getLevel() + 1, colIndex + 1,
						MessageService.getMessage("error.format.csvLayout.wrongColumn")));
				break;
			}
		}

		// 明細ヘッダ（2行構成）
		Row detailLevel0 = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_DETAIL_INDEX);
		Row detailLevel1 = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_DETAIL_INDEX + 1);

		for (CsvLayoutDetailEnum header : CsvLayoutDetailEnum.values()) {
			String expected = header.getDisplayName();
			int colIndex = header.getColumnIndex();
			String actual = "";
			Row targetRow = header.getLevel() == 0 ? detailLevel0 : detailLevel1;
			if (targetRow != null) {
				actual = getCellValue(targetRow, colIndex).trim();
			}
			if (!expected.equals(actual)) {
				errors.add(new ExcelParseError(sheet.getSheetName(),
						CSV_LAYOUT_SHEET.START_POS_DETAIL_INDEX + header.getLevel() + 1, colIndex + 1,
						MessageService.getMessage("error.format.csvLayout.wrongColumn")));
				break;
			}
		}
	}

	/**
	 * CSVレイアウト定義書のヘッダ領域（上部3行）からメタデータを抽出し、
	 * {@link CsvLayout} オブジェクトを構築します。
	 * 
	 * <p>各項目は固定列位置から取得されます（例: 機能名＝7列目、ファイルID＝21列目など）。
	 * 行が存在しない場合は該当フィールドを空文字列に設定します。
	 * 
	 * @param sheet 解析対象のExcelシート
	 * @return ヘッダメタデータを保持する {@link CsvLayout} オブジェクト
	 */
	private CsvLayout readHeaderMetadata(Sheet sheet) {
		CsvLayout layout = new CsvLayout();

		Row row0 = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX);
		if (row0 != null) {
			layout.setFunctionName(getCellValue(row0, 6));      // G列（7列目）
			layout.setFileId(getCellValue(row0, 20));           // U列（21列目）
			layout.setFileName(getCellValue(row0, 34));         // AJ列（35列目）
			layout.setInputOutputType(getCellValue(row0, 46));  // AV列（47列目）
			layout.setFileFormat(getCellValue(row0, 57));       // BI列（58列目）
		}

		Row row1 = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX + 1);
		if (row1 != null) {
			layout.setFileNamingRule(getCellValue(row1, 6));    // G列
			layout.setCharacterEncoding(getCellValue(row1, 46)); // AV列
			layout.setLineEncoding(getCellValue(row1, 57));     // FC列（158列目）
		}

		Row row2 = sheet.getRow(CSV_LAYOUT_SHEET.START_POS_HEADER_INDEX + 2);
		if (row2 != null) {
			layout.setSpecialNotes(getCellValue(row2, 6));      // G列
		}

		return layout;
	}
}