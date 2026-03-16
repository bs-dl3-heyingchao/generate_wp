package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.StringUtils;
import com.neusoft.bsdl.wptool.core.CommonConstant.SCREEN_FUNC_SPECIFICATION_SHEET;
import com.neusoft.bsdl.wptool.core.enums.ScreenFuncSpecificationHeaderEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenFuncSpecification;

/**
 * 画面機能定義書（Excel）のコンテンツを解析するためのツールクラス。
 * 
 * このクラスは、指定された Excel ファイルの「画面機能定義書」シートを読み込み、
 * ヘッダーの構造を検証した上で、データ行を ScreenFuncSpecification オブジェクトに変換して返します。
 * 
 * 解析プロセスは2段階で構成されます：
 * 1. ヘッダー行の検証（必須項目の存在・名称の一致）
 * 2. データ行の読み込み（EasyExcel を用いた非同期パース）
 * 
 * ヘッダーが不正な場合、エラーを収集して解析を中断し、null を返します。
 * データ行は「項番（itemNo）」が空でない行のみ有効とし、それ以外はスキップします。
 * 
 * 注意：このクラスは「画面機能定義書」専用の解析器であり、他のシート形式には対応していません。
 */
public class ScreenFuncSpecificationParseExcel extends AbstractParseTool {

    /**
     * 指定された Excel ファイルの「画面機能定義書」シートを解析し、
     * 有効な画面機能定義データのリストを返します。
     * 
     * 解析手順：
     * 1. 入力ストリームを用いてワークブックを生成
     * 2. 指定シートのヘッダー行を検証（必須列の名称・順序が正しいか）
     * 3. ヘッダー検証でエラーがあれば、解析を中止して null を返す
     * 4. もう一度入力ストリームをオープンし、EasyExcel でデータ行をパース
     * 5. 項番（itemNo）が空でない行のみを結果リストに追加
     * 
     * @param source   解析対象の Excel ファイル（FileSource オブジェクト）
     * @param sheetName 解析対象のシート名（例: "画面機能定義書"）
     * @param errors   エラー情報を格納するリスト（参照渡し）
     * @return 解析成功時は ScreenFuncSpecification のリスト、失敗時は null
     * @throws Exception 入力ストリームのオープンや Excel 解析中に発生した例外
     */
    public List<ScreenFuncSpecification> parseSpecSheet(FileSource source, String sheetName,
            List<ExcelParseError> errors) throws Exception {

        // 1. ヘッダー検証：まず入力ストリームを用いてワークブックを生成し、ヘッダーを検証
        try (InputStream validInputStream = source.getInputStream();
             Workbook workbook = WorkbookFactory.create(validInputStream)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                errors.add(new ExcelParseError(sheetName, 0, 0,
                        MessageService.getMessage("error.format.funcSpecification.sheetNotFound")));
                return null;
            }

            // ヘッダー構造の検証を実施（列名・順序の妥当性チェック）
            validateHeaders(sheet, errors);

            // ヘッダー検証でエラーが存在する場合は、解析を中止
            if (!CollectionUtils.isEmpty(errors)) {
                return null;
            }
        }

        // 2. データ行のパース：再び入力ストリームを開き、EasyExcel でデータを読み込む
        try (InputStream inputStream = source.getInputStream()) {
            List<ScreenFuncSpecification> result = Lists.newArrayList();

            // EasyExcel のイベントリスナー：行単位でデータを取得
            AnalysisEventListener<ScreenFuncSpecification> listener = new AnalysisEventListener<ScreenFuncSpecification>() {
                @Override
                public void invoke(ScreenFuncSpecification row, AnalysisContext context) {
                    // 項番（itemNo）が空でない行のみを有効とみなす
                    // （タイトル行や空行をスキップ）
                    if (row != null && !StringUtils.isEmpty(row.getItemNo())) {
                        result.add(row);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 解析完了後の処理（今回は不要）
                }
            };

            // EasyExcel による読み込み設定
            // - headRowNumber: ヘッダー行の位置（0ベースで指定）→ 2行目（インデックス2）をヘッダーとみなす
            // - sheetName: 解析対象シート名
            EasyExcel.read(inputStream, ScreenFuncSpecification.class, listener)
                    .sheet(sheetName)
                    .headRowNumber(SCREEN_FUNC_SPECIFICATION_SHEET.START_POS_DATA_INDEX)
                    .doRead();

            return result;
        }
    }

    /**
     * 「画面機能定義書」シートのヘッダー行が、仕様に従って正しく構成されているかを検証します。
     * 
     * 検証対象：
     * - ヘッダー行の位置：SCREEN_FUNC_SPECIFICATION_SHEET.START_POS_HEADER_INDEX（例：2行目）
     * - 必須列の順序・名称：ScreenFuncSpecificationHeaderEnum に定義された列名と一致するか
     * 
     * 検証方法：
     * 各列の期待される名称（displayName）と、実際のセル値を比較。
     * 不一致が発生した場合、エラーを errors リストに追加し、即座に検証を中断（break）。
     * 
     * 注意：複数の列が不一致であっても、最初の不一致のみを報告します。
     *       （複数エラーを報告したい場合は、break を削除し、continue に変更）
     * 
     * @param sheet     解析対象の Excel シート
     * @param errors    エラー情報を格納するリスト（参照渡し）
     */
    public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
        // ヘッダー行を取得（例：3行目 → インデックス2）
        Row headerRow = sheet.getRow(SCREEN_FUNC_SPECIFICATION_SHEET.START_POS_HEADER_INDEX);

        // 定義されたすべてのヘッダー列を順番に検証
        for (ScreenFuncSpecificationHeaderEnum header : ScreenFuncSpecificationHeaderEnum.values()) {
            String expectedName = header.getDisplayName(); // 期待される列名（例: "項番"）
            int expectedIndex = header.getColumnIndex();   // 期待される列インデックス（例: 0）

            // 実際のセル値を取得し、前後の空白を除去
            String actualName = getCellValue(headerRow, expectedIndex).trim();

            // 期待値と実際の値が一致しない場合、エラーを記録
            if (!expectedName.equals(actualName)) {
                errors.add(new ExcelParseError(
                        sheet.getSheetName(),                           // シート名
                        SCREEN_FUNC_SPECIFICATION_SHEET.START_POS_HEADER_INDEX + 1, // 行番号（1起点）
                        expectedIndex + 1,                              // 列番号（1起点）
                        MessageService.getMessage("error.format.funcSpecification.wrongColumn") // エラーメッセージ
                ));
                // 最初の不一致で検証を中断（複数エラーを報告する場合はこの break を削除）
                break;
            }
        }
    }
}
