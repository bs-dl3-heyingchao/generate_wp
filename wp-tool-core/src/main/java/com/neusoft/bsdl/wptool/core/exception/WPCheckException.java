package com.neusoft.bsdl.wptool.core.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 汎用的なCheckエラー情報を保持する例外クラスです。 複数のエラーを集約して、詳細メッセージとして返却できます。
 */
public class WPCheckException extends WPException {

    private static final long serialVersionUID = 1L;

    private final List<String> errors = new ArrayList<>();

    /**
     * デフォルトメッセージで例外を生成します。
     */
    public WPCheckException() {
        super("check error");
    }

    /**
     * 指定メッセージで例外を生成します。
     *
     * @param message 例外メッセージ
     */
    public WPCheckException(String message) {
        super(message);
    }

    /**
     * 既存のエラー一覧を指定して例外を生成します。
     *
     * @param errors エラー一覧
     */
    public WPCheckException(List<String> errors) {
        this();
        if (errors != null) {
            this.errors.addAll(errors);
        }
    }

    /**
     * エラー情報を1件追加します。
     *
     * @param errorMessage エラーメッセージ
     */
    public void addError(String errorMessage) {
        this.errors.add(errorMessage);
    }

    /**
     * 保持しているエラー一覧を返します（読み取り専用）。
     *
     * @return エラー一覧
     */
    public List<String> getErrors() {
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
        StringBuilder builder = new StringBuilder(baseMessage == null ? "check error" : baseMessage);
        builder.append(". details=");
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append("message=").append(errors.get(i) == null ? "unknown" : errors.get(i));
        }
        return builder.toString();
    }

}
