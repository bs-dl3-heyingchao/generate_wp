package com.neusoft.bsdl.wptool.core.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.neusoft.bsdl.wptool.core.CommonConstant.SCREEN_ITEM_DESCRIPTION_SHEET;
import com.neusoft.bsdl.wptool.core.enums.ScreenItemDescriptionHeaderEnum;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException.ExcelParseError;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescription;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescriptionResult;

/**
 * 画面項目説明書（Excel）のコンテンツを解析するためのツールクラス。
 * 
 * このクラスは、2段階ヘッダー構造（レベル0：グループ名、レベル1：項目名）を持つ
 * 「画面項目説明書」シートを解析し、以下の構造に変換して返却します：
 * 
 * ┌───────────────────────┐
 * │ グループ名: "01. ログイン" │ ← ScreenItemDescriptionResult
 * ├───────────────────────┤
 * │ 項目1: 項番="1", 項目名="ID"     │ ← ScreenItemDescription
 * │ 項目2: 項番="2", 項目名="パスワード" │
 * │ 項目3: 項番="3", 項目名="認証"     │
 * └───────────────────────┘
 * 
 * 解析手順：
 * 1. ヘッダー行（2行）の構造を検証（レベル0・レベル1の列名が仕様と一致するか）
 * 2. データ行を読み込み、グループ名（項番のみ存在し、項目名が空）でグループを区切る
 * 3. 各グループに属する項目をリスト化し、ScreenItemDescriptionResult として返却
 * 
 * 注意：入力 Excel は必ず「2行ヘッダー」形式で構成されていることを前提としています。
 */
public class ScreenItemDescriptionParseExcel extends AbstractParseTool {

    /**
     * 画面項目説明書の Excel ファイルを解析し、グループ化された結果を返します。
     * 
     * 解析フロー：
     * 1. 入力ストリームからワークブックを生成し、ヘッダー行（2行）を検証
     * 2. ヘッダー検証でエラーが発生した場合、null を返して解析を中止
     * 3. 再度入力ストリームを開き、EasyExcel でデータ行をパース
     * 4. グループ化リスナー（GroupingListener）が、項目名が空の行を「グループ名」として認識
     * 5. グループごとに項目をまとめ、ScreenItemDescriptionResult として返却
     * 
     * @param source   解析対象の Excel ファイル（FileSource）
     * @param sheetName 解析対象のシート名（例: "画面項目説明書"）
     * @param errors   エラー情報を格納するリスト（参照渡し）
     * @return 解析成功時は ScreenItemDescriptionResult のリスト、失敗時は null
     * @throws Exception 入力ストリームのオープン、Excel パース中に発生した例外
     */
    public List<ScreenItemDescriptionResult> parseSpecSheet(FileSource source, String sheetName,
            List<ExcelParseError> errors) throws Exception {

        // 1. ヘッダー構造の検証（レベル0とレベル1の2行をチェック）
        try (InputStream validInputStream = source.getInputStream();
             Workbook workbook = WorkbookFactory.create(validInputStream)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                errors.add(new ExcelParseError(sheetName, 0, 0,
                        MessageService.getMessage("error.format.itemDescription.sheetNotFound")));
                return null;
            }

            validateHeaders(sheet, errors);

            // ヘッダー検証でエラーが存在する場合、解析を中止
            if (!CollectionUtils.isEmpty(errors)) {
                return null;
            }
        }

        // 2. データ行の解析：グループ化リスナーを用いて項目をまとめる
        try (InputStream is2 = source.getInputStream()) {
            GroupingListener listener = new GroupingListener();
            EasyExcel.read(is2, ScreenItemDescription.class, listener)
                    .sheet(sheetName)
                    .headRowNumber(SCREEN_ITEM_DESCRIPTION_SHEET.START_POS_DATA_INDEX)
                    .doRead();

            return listener.getResult();
        }
    }

    /**
     * 「画面項目説明書」シートのヘッダー行（2行）の構造を検証します。
     * 
     * 検証対象：
     * - レベル0ヘッダー（例：第3行）：グループ名（項番）を示す列
     * - レベル1ヘッダー（例：第4行）：項目名・入力形式・必須・説明などの列
     * 
     * ScreenItemDescriptionHeaderEnum に定義された各列の：
     * - level（0または1）に応じて、対応するヘッダー行から値を取得
     * - 期待される列名（displayName）と実際のセル値を比較
     * - 不一致が発生した場合、エラーを errors に追加し、即座に検証を中断（break）
     * 
     * 注意：複数の列が不一致であっても、最初の不一致のみを報告します。
     *       （複数エラーを報告したい場合は、break を削除して continue に変更）
     * 
     * @param sheet     解析対象の Excel シート
     * @param errors    エラー情報を格納するリスト（参照渡し）
     */
    public static void validateHeaders(Sheet sheet, List<ExcelParseError> errors) {
        // ヘッダー行を取得（レベル0：2行目、レベル1：3行目）
        Row level0HeaderRow = sheet.getRow(SCREEN_ITEM_DESCRIPTION_SHEET.START_POS_HEADER_INDEX);
        Row level1HeaderRow = sheet.getRow(SCREEN_ITEM_DESCRIPTION_SHEET.START_POS_HEADER_INDEX + 1);

        // 定義されたすべてのヘッダー列を順番に検証
        for (ScreenItemDescriptionHeaderEnum header : ScreenItemDescriptionHeaderEnum.values()) {
            String expectedName = header.getDisplayName(); // 期待される列名（例: "項番"）
            int expectedIndex = header.getColumnIndex();   // 期待される列インデックス（例: 0）
            String actualName = "";

            // ヘッダーのレベルに応じて、対応する行から値を取得
            if (header.getLevel() == 0) {
                actualName = getCellValue(level0HeaderRow, expectedIndex).trim();
            } else {
                actualName = getCellValue(level1HeaderRow, expectedIndex).trim();
            }

            // 期待値と実際の値が一致しない場合、エラーを記録
            if (!expectedName.equals(actualName)) {
                errors.add(new ExcelParseError(
                        sheet.getSheetName(),
                        SCREEN_ITEM_DESCRIPTION_SHEET.START_POS_HEADER_INDEX + 1, // 行番号（1起点）
                        expectedIndex + 1,                                      // 列番号（1起点）
                        MessageService.getMessage("error.format.itemDescription.wrongColumn")
                ));
                // 最初の不一致で検証を中断（複数エラーを報告する場合はこの break を削除）
                break;
            }
        }
    }

    /**
     * 画面項目説明書のデータ行をグループ化して処理するための EasyExcel リスナー。
     * 
     * ロジックの説明：
     * - グループ名は「項番（itemNo）」が存在し、「項目名（itemName）」が空の行で定義される
     * - 項目は「項番」と「項目名」の両方が存在する行で定義される
     * - 各グループ（例: "01. ログイン"）に対して、関連する項目をリスト化し、
     *   ScreenItemDescriptionResult として保持する
     * 
     * グループ化の例：
     * | 項番 | 項目名 | 入力形式 |
     * |------|--------|----------|
     * | 01. ログイン |        |          | ← グループ開始
     * | 1    | ID     | 文字列   |
     * | 2    | パスワード | 文字列   |
     * | 02. 会員登録 |        |          | ← グループ切り替え
     * | 1    | 名前   | 文字列   |
     * 
     * 結果：[ { groupName="01. ログイン", items=[{itemNo="1", itemName="ID"}, ...] }, ... ]
     */
    public static class GroupingListener extends AnalysisEventListener<ScreenItemDescription> {

        // 現在処理中のグループ名（例: "01. ログイン"）
        private String currentGroupName = null;

        // 現在のグループに属する項目リスト
        private List<ScreenItemDescription> currentItems = new ArrayList<>();

        // 最終的な結果（グループ化された結果のリスト）
        private final List<ScreenItemDescriptionResult> result = new ArrayList<>();

        /**
         * 各データ行（1行）が読み込まれるたびに呼び出される。
         * 
         * ロジック：
         * 1. 項番と項目名を取得し、空文字を安全に扱う
         * 2. 項番あり・項目名なし → グループ名として認識（新しいグループ開始）
         * 3. 項番あり・項目名あり → 項目としてリストに追加（数値チェックも実施）
         * 4. 項番なし → スキップ（空行や余計な行）
         * 
         * 注意：項番は整数値である必要がある（例: "1"、"2"）。
         *       整数変換に失敗した場合は無効な行とみなしてスキップ。
         */
        @Override
        public void invoke(ScreenItemDescription row, AnalysisContext context) {
            if (row == null) {
                return;
            }

            // 項番と項目名を安全に取得（null対策）
            String itemNo = Objects.toString(row.getItemNo(), "").trim();
            String itemName = Objects.toString(row.getItemName(), "").trim();

            // グループ名の判定：項番あり、項目名なし → 新しいグループ
            if (!itemNo.isEmpty() && itemName.isEmpty()) {
                // 前のグループを保存（存在する場合）
                saveCurrentGroup();
                // 新しいグループを開始
                this.currentGroupName = itemNo;
                this.currentItems = new ArrayList<>();
            }
            // 項目の判定：項番あり、項目名あり → 項目を追加
            else if (!itemNo.isEmpty() && !itemName.isEmpty()) {
                try {
                    // 項番は整数であることを必須とし、変換不能なら無効行とみなす
                    Integer.parseInt(itemNo);
                    currentItems.add(row);
                } catch (NumberFormatException e) {
                    // 項番が整数でない場合は無効行として無視
                    // （ログ出力が必要ならここに logger.warn を追加可能）
                }
            }
        }

        /**
         * 現在のグループ（currentGroupName + currentItems）を結果リストに保存します。
         * グループ名または項目リストが空の場合は保存しません。
         * 
         * 注意：ディープコピー（new ArrayList<>(currentItems)）を行って、
         *       リスナーの内部状態が後続処理に影響しないようにしています。
         */
        private void saveCurrentGroup() {
            if (currentGroupName != null && !currentItems.isEmpty()) {
                ScreenItemDescriptionResult group = new ScreenItemDescriptionResult();
                group.setGroupName(currentGroupName);
                group.setItems(new ArrayList<>(currentItems)); // ディープコピー
                result.add(group);
            }
        }

        /**
         * 全行の解析が完了した後に呼び出されます。
         * 最後のグループを保存するために、このメソッドで saveCurrentGroup() を呼び出します。
         */
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            saveCurrentGroup();
        }

        /**
         * 解析結果を返却します。
         * 外部から取得する際は、内部リストのコピーを返すことで、
         * 外部からの変更が内部状態に影響しないようにします。
         * 
         * @return グループ化された ScreenItemDescriptionResult のリスト
         */
        public List<ScreenItemDescriptionResult> getResult() {
            return new ArrayList<>(result);
        }
    }
}
