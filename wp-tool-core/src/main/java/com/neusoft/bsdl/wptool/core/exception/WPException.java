package com.neusoft.bsdl.wptool.core.exception;

/**
 * Excel解析時のエラー情報を保持する例外クラスです。 複数のエラーを集約して、詳細メッセージとして返却できます。
 */
public class WPException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * デフォルトメッセージで例外を生成します。
     */
    public WPException() {
        super("Wp error");
    }

    /**
     * 指定メッセージで例外を生成します。
     *
     * @param message 例外メッセージ
     */
    public WPException(String message) {
        super(message);
    }

    public WPException(String message, Throwable cause) {
        super(message, cause);
    }

}
