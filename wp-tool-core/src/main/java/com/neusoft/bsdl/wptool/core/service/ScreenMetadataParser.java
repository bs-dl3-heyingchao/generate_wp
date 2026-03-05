package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * 画面仕様書のヘッダーメタデータ（第2行）を解析するサービス
 */
@Slf4j
public class ScreenMetadataParser {

    /**
     * Excel の第2行（0-based index = 1）からメタデータを読み取る
     *
     * @param source Excelファイルソース
     * @param sheetName 対象シート名
     * @return ScreenMetadata オブジェクト
     * @throws Exception 読み込みエラー
     */
    public static ScreenMetadata readHeaderMetadata(FileSource source, String sheetName) throws Exception {
        try (InputStream is = source.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("シートが見つかりません: " + sheetName);
            }

            Row dataRow = sheet.getRow(2);
            if (dataRow == null) {
                log.warn("第2行にデータがありません。シート: {}", sheetName);
                return new ScreenMetadata(); // 空オブジェクト返す
            }

            ScreenMetadata meta = new ScreenMetadata();
            //システム
            meta.setSystem(getCellValue(dataRow, 0));
            //サブシステム
            meta.setSubSystem(getCellValue(dataRow, 7));
            //フェーズ
            meta.setPhase(getCellValue(dataRow, 14));
            //ドキュメント名
            meta.setDocumentName(getCellValue(dataRow, 22));
            //機能分類
            meta.setFunctionType(getCellValue(dataRow, 31));
            //機能ID
            meta.setFunctionId(getCellValue(dataRow, 46));
            //画面ID
            meta.setScreenId(getCellValue(dataRow, 53));
            //画面名
            meta.setScreenName(getCellValue(dataRow, 63));

            log.debug("ヘッダーメタデータ読み込み完了: {}", meta);
            return meta;
        }
    }

    /**
     * セルの値を安全に文字列として取得
     */
    private static String getCellValue(Row row, int columnIndex) {
        if (row == null) return "";
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return "";

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