package com.neusoft.bsdl.wptool.core.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WPParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<ExcelParseError> errors = new ArrayList<>();

    public WPParseException() {
        super("Excel parse error");
    }

    public WPParseException(String message) {
        super(message);
    }

    public WPParseException(String sheetName, Integer rowIndex, Integer columnIndex, String errorMessage) {
        this();
        addError(sheetName, rowIndex, columnIndex, errorMessage);
    }

    public WPParseException(List<ExcelParseError> errors) {
        this();
        if (errors != null) {
            this.errors.addAll(errors);
        }
    }

    public void addError(String sheetName, Integer rowIndex, Integer columnIndex, String errorMessage) {
        this.errors.add(new ExcelParseError(sheetName, rowIndex, columnIndex, errorMessage));
    }

    public void addError(ExcelParseError error) {
        this.errors.add(Objects.requireNonNull(error, "error must not be null"));
    }

    public List<ExcelParseError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

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
        private final Integer rowIndex;
        private final Integer columnIndex;
        private final String errorMessage;

        public ExcelParseError(String sheetName, Integer rowIndex, Integer columnIndex, String errorMessage) {
            this.sheetName = sheetName;
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
            this.errorMessage = errorMessage;
        }

        public String getSheetName() {
            return sheetName;
        }

        public Integer getRowIndex() {
            return rowIndex;
        }

        public Integer getColumnIndex() {
            return columnIndex;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String format() {
            StringBuilder location = new StringBuilder();
            location.append("sheet=").append(sheetName == null ? "unknown" : sheetName);
            if (rowIndex != null) {
                location.append(", row=").append(rowIndex);
            }
            if (columnIndex != null) {
                location.append(", col=").append(columnIndex);
            }
            location.append(", message=").append(errorMessage == null ? "unknown" : errorMessage);
            return location.toString();
        }
    }

}
