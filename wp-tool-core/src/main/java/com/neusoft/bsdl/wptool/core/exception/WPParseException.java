package com.neusoft.bsdl.wptool.core.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Excel解析時のエラー情報を保持する例外クラスです。 複数のエラーを集約して、詳細メッセージとして返却できます。
 */
public class WPParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<ExcelParseError> errors = new ArrayList<>();

    /**
     * デフォルトメッセージで例外を生成します。
     */
    public WPParseException() {
        super("Excel parse error");
    }

    /**
     * 指定メッセージで例外を生成します。
     *
     * @param message 例外メッセージ
     */
    public WPParseException(String message) {
        super(message);
    }

    /**
     * 単一エラー情報を指定して例外を生成します。
     *
     * @param sheetName    シート名
     * @param rowNumber    行番号（1始まり）
     * @param colNumber    列番号（1始まり）
     * @param errorMessage エラーメッセージ
     */
    public WPParseException(String sheetName, Integer rowNumber, Integer colNumber, String errorMessage) {
        this();
        addError(sheetName, rowNumber, colNumber, errorMessage);
    }

    /**
     * 既存のエラー一覧を指定して例外を生成します。
     *
     * @param errors エラー一覧
     */
    public WPParseException(List<ExcelParseError> errors) {
        this();
        if (errors != null) {
            this.errors.addAll(errors);
        }
    }

    /**
     * エラー情報を1件追加します。
     *
     * @param sheetName    シート名
     * @param rowNumber    行番号（1始まり）
     * @param colNumber    列番号（1始まり）
     * @param errorMessage エラーメッセージ
     */
    public void addError(String sheetName, Integer rowNumber, Integer colNumber, String errorMessage) {
        this.errors.add(new ExcelParseError(sheetName, rowNumber, colNumber, errorMessage));
    }

    /**
     * エラー情報を1件追加します。
     *
     * @param error エラー情報
     */
    public void addError(ExcelParseError error) {
        this.errors.add(Objects.requireNonNull(error, "error must not be null"));
    }

    /**
     * 保持しているエラー一覧を返します（読み取り専用）。
     *
     * @return エラー一覧
     */
    public List<ExcelParseError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * エラーを保持しているかを返します。
     *
     * @return エラーが存在する場合は true
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 基本メッセージにエラー詳細を付与したメッセージを返します。
     *
     * @return 例外メッセージ
     */
    @Override
    public String getMessage() {
        String baseMessage = super.getMessage();
        if (errors.isEmpty()) {
            return baseMessage;
        }
        StringBuilder builder = new StringBuilder(baseMessage == null ? "Excel parse error" : baseMessage);
        builder.append(". details=");
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(errors.get(i).format());
        }
        return builder.toString();
    }

    public static class ExcelParseError {
        private final String sheetName;
        private final Integer rowNumber;
        private final Integer colNumber;
        private final String errorMessage;

        public ExcelParseError(String sheetName, Integer rowNumber, Integer colNumber, String errorMessage) {
            this.sheetName = sheetName;
            this.rowNumber = rowNumber;
            this.colNumber = colNumber;
            this.errorMessage = errorMessage;
        }

        public String getSheetName() {
            return sheetName;
        }

        /**
         * エラー発生行番号（1始まり）を返します。
         *
         * @return 行番号（1始まり）
         */
        public Integer getRowNumber() {
            return rowNumber;
        }

        /**
         * エラー発生列番号（1始まり）を返します。
         *
         * @return 列番号（1始まり）
         */
        public Integer getColNumber() {
            return colNumber;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String format() {
            StringBuilder location = new StringBuilder();
            location.append("sheet=").append(sheetName == null ? "unknown" : sheetName);
            if (rowNumber != null) {
                location.append(", row=").append(rowNumber);
            }
            if (colNumber != null) {
                location.append(", col=").append(colNumber);
            }
            if (rowNumber != null && colNumber != null) {
                location.append(" (").append(getCellAddress(colNumber, rowNumber)).append(")");
            }
            location.append(", message=").append(errorMessage == null ? "unknown" : errorMessage);
            return location.toString();
        }
    }

    /**
     * 1始まりの列番号を、Excel形式の列記号（A, B, ..., Z, AA, ...）へ変換します。 例: 4 -&gt; D
     *
     * @param colNumber 列番号（1始まり）
     * @return Excel列記号
     */
    public static String colNumToLetter(int colNumber) {
        String colRef = "";
        int colRemain = colNumber;
        while (colRemain > 0) {
            int thisPart = colRemain % 26;
            if (thisPart == 0) {
                thisPart = 26;
            }
            colRemain = (colRemain - thisPart) / 26;
            char colChar = (char) (thisPart + 64); // The letter A is at 65
            colRef = colChar + colRef;
        }
        return colRef;
    }

    /**
     * 列番号・行番号（ともに1始まり）からセルアドレスを生成します。 例: 4,1 -&gt; D1
     *
     * @param colNumber 列番号（1始まり）
     * @param rowNumber 行番号（1始まり）
     * @return セルアドレス
     */
    public static String getCellAddress(int colNumber, int rowNumber) {
        return colNumToLetter(colNumber) + rowNumber;
    }

}
