package com.neusoft.bsdl.wptool.core.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 複数の message.properties ファイルを読み込み、それらをマージして利用するためのサービスクラス。
 * 
 * このクラスは、複数のモジュール（例：Aプロジェクト、Bプロジェクト）から同じ名前の
 * message.properties ファイルが存在する場合でも、すべてを読み込んで1つのプロパティセットに統合します。
 * これにより、複数のモジュールで共通のメッセージ定義を一元管理でき、
 * 重複や矛盾を防ぐことができます。
 * 
 * 読み込み順序はクラスローダーの検索順に依存します（後から読み込まれた値が優先されます）。
 */
public class MessageService {
    private static final Properties properties = new Properties();

    static {
        try {
        	 // クラスローダーを通じて、classpath 内のすべての "message.properties" を検索
            Enumeration<URL> resources = MessageService.class.getClassLoader()
                    .getResources("message.properties");

         // 検出されたすべてのファイルを順次読み込む
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (InputStream inputStream = url.openStream()) {
                    // UTF-8 エンコードで読み込み、properties にマージ
                    properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                }
            }

         // 1つも message.properties が見つからなかった場合、起動エラーとして通知
            if (properties.isEmpty()) {
                throw new RuntimeException("message.properties ファイルが見つかりませんでした。" +
                        "アプリケーションの classpath に message.properties を配置してください。");
            }
        } catch (IOException e) {
        	// 読み込み中に I/O エラーが発生した場合、原因を含めて例外を再スロー
            throw new RuntimeException("message.properties の読み込みに失敗しました。", e);
        }
    }
    
    /**
     * 指定されたキーに対応するメッセージ文字列を返します。
     * キーが見つからない場合は、"MISSING_MESSAGE_KEY: [キー名]" の形式でデフォルト値を返します。
     * 
     * @param key メッセージのキー（例: "error.login.failed"）
     * @return メッセージ文字列、またはキーが存在しない場合のエラーメッセージ
     */
    public static String getMessage(String key) {
        return properties.getProperty(key, "MISSING_MESSAGE_KEY: " + key);
    }

    /**
     * 指定されたキーに対応するメッセージ文字列を取得し、引数で指定された値を用いて
     * メッセージをフォーマット（置換）して返します。
     * 
     * Java の String.format() を使用して、{0}, {1}, %s などのプレースホルダーを置換します。
     * 
     * キーが見つからない場合は、"MISSING_MESSAGE_KEY: [キー名]" を返します。
     * 
     * @param key メッセージのキー
     * @param args メッセージ内のプレースホルダーに置き換える引数（可変長）
     * @return フォーマット済みのメッセージ、またはエラーメッセージ
     */
    public static String getMessage(String key, Object... args) {
        String msg = properties.getProperty(key);
        if (msg == null) {
            return "MISSING_MESSAGE_KEY: " + key;
        }
        return String.format(msg, args);
    }
}