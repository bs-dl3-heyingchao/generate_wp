package com.neusoft.bsdl.wptool.core.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.StringUtils;
import com.neusoft.bsdl.wptool.core.CommonConstant.SCREEN_VALIDATION_SHEET;
import com.neusoft.bsdl.wptool.core.enums.ScreenValidationHeaderEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenValidation;
import com.neusoft.bsdl.wptool.core.model.ScreenValidationAction;

import lombok.extern.slf4j.Slf4j;

/**
 * 画面チェック仕様書（Excel）のコンテンツを解析するためのサービスクラス。
 * 
 * このクラスは、複雑な構造を持つ「画面チェック仕様書」Excelファイルを解析し、
 * 以下の情報構造に変換して返却します：
 * 
 * ┌──────────────────────────────────────────────────────────────────────────────┐
 * │ ScreenValidation（1行 = 1チェック項目）                                        │
 * │ ├─ 基本情報：項番、項目名、チェック名、タイプ、ルール、メッセージID、メッセージ内容 │
 * │ ├─ パラメータ：Parameter1～5（チェック条件に使用する値）                          │
 * │ ├─ 補足情報：備考、ビジネス警告、コーディングメモ、その他メモ                     │
 * │ └─ アクションリスト：チェックアクション（50列～78列、間隔2列）の有無（○/空白）     │
 * └──────────────────────────────────────────────────────────────────────────────┘
 * 
 * 解析手順：
 * 1. Excelファイルをバイト配列として読み込み（複数回使用するため）
 * 2. ヘッダー行（レベル0・レベル1）の構造を検証（列名・順序の妥当性）
 * 3. アクション列の名前（例: "必須入力"、"数値チェック"）を第4行（index=3）から抽出
 * 4. データ行をパースし、各列の値を ScreenValidation にマッピング
 * 5. アクション列（50, 52, 54... 78列）を2列間隔で読み取り、○（MARK）があれば有効と判定
 * 
 * 注意：このExcelは「2段階ヘッダー」かつ「アクション列が間隔2列で配置」される特殊構造です。
 *       他の仕様書（例：画面機能定義書）とは構造が異なります。
 */
@Slf4j
public class ScreenValidationParseExcel extends AbstractParseTool {

    /**
     * 画面チェック仕様書のExcelを解析し、ScreenValidationのリストを返します。
     * 
     * 解析フロー：
     * 1. 入力ストリームからExcel全体をバイト配列として読み込む（後続処理で再利用）
     * 2. ワークブックを生成し、ヘッダー行（2行）の構造を検証
     *    → 検証失敗時は null を返し、エラーを errors に追加
     * 3. アクション列の名前（例: "必須入力"）を第4行（index=3）から抽出
     * 4. EasyExcel でデータ行をパース（headRowNumber=5：第6行からデータ開始）
     * 5. 各行を ScreenValidation オブジェクトに変換し、アクションリストを構築
     * 6. 項番が空または数値でない行は無視（無効行スキップ）
     * 
     * @param source   解析対象の Excel ファイル（FileSource）
     * @param sheetName 解析対象のシート名（例: "画面チェック仕様書"）
     * @param errors   エラー情報を格納するリスト（参照渡し）
     * @return 解析成功時は ScreenValidation のリスト、失敗時は null
     * @throws Exception Excelの読み込み・パース中に発生した例外
     */
    public List<ScreenValidation> parseSpecSheet(FileSource source, String sheetName, List<ExcelParseError> errors)
            throws Exception {

        // Excelファイル全体をバイト配列として読み込む（ヘッダー検証とデータ解析で再利用）
        byte[] excelBytes;
        try (InputStream is = source.getInputStream()) {
            excelBytes = is.readAllBytes();
        }

        // 1. ヘッダー構造の検証（レベル0とレベル1の2行）
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                errors.add(new ExcelParseError(sheetName, 0, 0,
                        MessageService.getMessage("error.format.validation.sheetNotFound")));
                return null;
            }
            validateHeaders(sheet, errors);
            if (!CollectionUtils.isEmpty(errors)) {
                return null; // ヘッダー検証失敗時は解析を中止
            }
        }

        // 2. アクション列の名前を抽出（第4行：index=3、列番号50～78、間隔2列）
        List<String> actionColumnNames = readActionColumnNames(excelBytes, sheetName);

        // 3. データ行の解析：EasyExcel でマップ形式で読み込み
        List<ScreenValidation> result = new ArrayList<>();
        try (InputStream bis = new ByteArrayInputStream(excelBytes)) {
            EasyExcel.read(bis, new AnalysisEventListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> rowMap, AnalysisContext context) {
                    // 項番（列0）が空または数値でない場合は無効行としてスキップ
                    String itemNo = rowMap.get(0);
                    if (!isValidItemNo(itemNo)) {
                        return;
                    }

                    // マップからデータを抽出し、ScreenValidationオブジェクトを構築
                    ScreenValidation validation = buildScreenValidation(rowMap, actionColumnNames);
                    result.add(validation);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 解析完了後の処理（なし）
                }
            }).sheet(sheetName)
              .headRowNumber(SCREEN_VALIDATION_SHEET.START_POS_DATA_INDEX) // データは第6行から（index=5）
              .doRead();
        }

        return result;
    }

    /**
     * 「画面チェック仕様書」シートのヘッダー構造を検証します。
     * 
     * 検証対象：
     * - レベル0ヘッダー（第3行：index=2）：「項目番号」「項目名」などの大分類
     * - レベル1ヘッダー（第4行：index=3）：「入力形式」「必須」「説明」などの詳細分類
     * 
     * ScreenValidationHeaderEnum に定義された各列について：
     * - getLevel() で対応するヘッダー行（レベル0または1）を決定
     * - getDisplayName() で期待される列名を取得
     * - getColumnInfo() で期待される列インデックスを取得
     * - 実際のセル値と比較し、不一致があればエラーを記録
     * 
     * 注意：最初の不一致で検証を中断（break）しています。
     *       複数の列が不一致でも、最初の1つだけを報告する設計です。
     *       （すべての不一致を報告したい場合は、break を削除してください）
     * 
     * @param sheet     解析対象の Excel シート
     * @param errors    エラー情報を格納するリスト（参照渡し）
     */
    public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
        // ヘッダー行を取得
        Row level0Header = sheet.getRow(SCREEN_VALIDATION_SHEET.START_POS_HEADER_INDEX);     // 第3行
        Row level1Header = sheet.getRow(SCREEN_VALIDATION_SHEET.START_POS_HEADER_INDEX + 1); // 第4行

        // 定義されたすべてのヘッダー列を検証
        for (ScreenValidationHeaderEnum header : ScreenValidationHeaderEnum.values()) {
            String expectedName = header.getDisplayName(); // 期待される列名（例: "項目番号"）
            int colIndex = header.getColumnIndex();        // 期待される列インデックス（例: 0）
            String actualName = "";

            // ヘッダーのレベルに応じて、対応する行を選択
            Row targetRow = (header.getLevel() == 0) ? level0Header : level1Header;
            if (targetRow != null) {
                actualName = getCellValue(targetRow, colIndex).trim();
            }

            // 期待値と実際の値が一致しない場合、エラーを記録
            if (!expectedName.equals(actualName)) {
                // エラー行番号：レベル0なら3行目、レベル1なら4行目（1起点）
                int errorRow = SCREEN_VALIDATION_SHEET.START_POS_HEADER_INDEX + header.getLevel() + 1;
                errors.add(new ExcelParseError(
                        sheet.getSheetName(),
                        errorRow,                 // 行番号（1起点）
                        colIndex + 1,             // 列番号（1起点）
                        MessageService.getMessage("error.format.validation.wrongColumn")
                ));
                // 最初の不一致で検証を中断（複数エラーを報告する場合はこの break を削除）
                break;
            }
        }
    }

    /**
     * アクション列の名前（例: "必須入力"、"数値チェック"）を抽出します。
     * 
     * 構造：
     * - アクション列は第4行（index=3）に存在
     * - 列番号は 50, 52, 54, ..., 78（2列間隔）
     * - 総数は SCREEN_VALIDATION_SHEET.ACTION_COLUMN_COUNT（例: 15列）
     * 
     * ロジック：
     * 1. Excelを再度開き、第4行を取得
     * 2. 指定された列番号から順にセル値を読み取り、trim() で空白除去
     * 3. 空文字の場合は空文字を保持（アクション名が未定義の可能性あり）
     * 4. 列番号は2列ずつスキップ（アクション列は「チェック有無」列と交互に配置）
     * 
     * 注意：このメソッドは、データ行のパース前に一度だけ実行されます。
     *       アクション列の名前は、ScreenValidationAction の actionName に使用されます。
     * 
     * @param excelBytes Excelファイルのバイト配列
     * @param sheetName 解析対象のシート名
     * @return アクション列の名前リスト（例: ["必須入力", "数値チェック", ...]）
     * @throws Exception Excelの読み込みに失敗した場合
     */
    private List<String> readActionColumnNames(byte[] excelBytes, String sheetName) throws Exception {
        List<String> names = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                log.warn("シート '{}' が見つかりません。アクション列の抽出をスキップします。", sheetName);
                return names;
            }

            Row headerRow = sheet.getRow(SCREEN_VALIDATION_SHEET.START_POS_HEADER_INDEX + 1); // 第4行（index=3）
            if (headerRow == null) {
                log.warn("チェックアクションヘッダ行（第5行）が存在しません。アクション列を空リストとして返します。");
                return names;
            }

            int startCol = SCREEN_VALIDATION_SHEET.ACTION_START_INDEX; // 例: 50
            int count = SCREEN_VALIDATION_SHEET.ACTION_COLUMN_COUNT;   // 例: 15

            for (int i = 0; i < count; i++) {
                Cell cell = headerRow.getCell(startCol + (i * 2)); // 2列間隔：50, 52, 54...
                String value = (cell != null) ? cell.toString().trim() : "";
                names.add(value);
            }
        }
        return names;
    }

    /**
     * EasyExcel で読み込んだ1行のデータ（Map<Integer, String>）を ScreenValidation オブジェクトに変換します。
     * 
     * マッピング規則：
     * - 列番号（0～210）は、Excelの列インデックスと1対1対応
     * - アクション列（50～78）は、readActionColumnNames() で取得した名前リストと連動
     * - アクションの値は「○」（MARK）があれば true、それ以外は false
     * 
     * パラメータ列（111, 117, 123...）は、チェックルールで使用する変数値（例: 最大文字数、許可値）
     * 
     * @param rowMap   EasyExcel が読み込んだ1行のデータ（キー=列番号, 値=文字列）
     * @param actionNames アクション列の名前リスト（例: ["必須入力", "数値チェック", ...]）
     * @return 構築された ScreenValidation オブジェクト
     */
    private ScreenValidation buildScreenValidation(Map<Integer, String> rowMap, List<String> actionNames) {
        ScreenValidation v = new ScreenValidation();

        // 基本情報のマッピング
        v.setItemNo(rowMap.get(0));                    // 項番
        v.setItemName(rowMap.get(1));                  // 項目名
        v.setValidationName(rowMap.get(7));            // チェック名
        v.setType(rowMap.get(18));                     // タイプ（例: 入力チェック）
        v.setValidationRule(rowMap.get(22));           // チェックルール（例: notBlank, maxLength=10）
        v.setMessageId(rowMap.get(80));                // メッセージID（i18n用キー）
        v.setMessageContent(rowMap.get(85));           // メッセージ内容（直接記述）
        v.setParameter1(rowMap.get(111));              // パラメータ1（例: 最大長）
        v.setParameter2(rowMap.get(117));              // パラメータ2
        v.setParameter3(rowMap.get(123));              // パラメータ3
        v.setParameter4(rowMap.get(129));              // パラメータ4
        v.setParameter5(rowMap.get(135));              // パラメータ5
        v.setRemarks(rowMap.get(141));                 // 備考
        v.setBizWarining(rowMap.get(177));             // ビジネス警告
        v.setCodingMemo(rowMap.get(178));              // コーディングメモ
        v.setMemo(rowMap.get(210));                    // その他メモ

        // アクションリストの構築
        List<ScreenValidationAction> actions = new ArrayList<>();
        int colIndex = SCREEN_VALIDATION_SHEET.ACTION_START_INDEX; // 例: 50

        for (int i = 0; i < actionNames.size(); i++) {
            String actionName = actionNames.get(i);

            // アクション名が空なら以降をスキップ（末尾の空列を無視）
            if (StringUtils.isEmpty(actionName)) {
                break;
            }

            // アクションのチェック有無（○）は、対応する列の値で判定
            String cellValue = rowMap.get(colIndex);
            ScreenValidationAction action = new ScreenValidationAction();
            action.setActionName(actionName);
            action.setHasChecked(SCREEN_VALIDATION_SHEET.MARK.equals(cellValue)); // "○" なら true

            actions.add(action);
            colIndex += 2; // 次のアクション列は2列先（間隔2列）
        }

        v.setValidationActions(actions);
        return v;
    }

    /**
     * 項番（itemNo）が有効かどうかを判定します。
     * 
     * 有効条件：
     * - null でないこと
     * - 空白文字だけではないこと
     * - 整数に変換可能であること（例: "1", "2", "100"）
     * 
     * 無効な項番（例: "A", "", "1.5"）は、データ行として無視されます。
     * 
     * @param itemNo 項番の文字列
     * @return 有効な項番の場合は true、それ以外は false
     */
    private boolean isValidItemNo(String itemNo) {
        if (itemNo == null || itemNo.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(itemNo.trim());
            return true;
        } catch (NumberFormatException e) {
            // 整数に変換できない場合は無効行とみなす
            log.debug("無効な項番を検出: {}", itemNo);
            return false;
        }
    }
}
