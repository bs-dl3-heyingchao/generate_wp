package com.neusoft.bsdl.wptool.core.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * AIサポートAPIから返却される個別のファイルデータを表現するクラス。
 * <p>
 * 入力ファイル（例: Markdown設計書）がAIによって解析・構造化された結果を保持します。
 * 特に、構造化されたJSONデータは {@code binaryCode} フィールドとしてAPIから提供されますが、
 * 本クラスではそれを文字列として {@link #binaryCodeJson} に格納し、後続処理でパース・整形します。
 * </p>
 */
@Data
public class FileData {

    /**
     * 元となった入力ファイルのファイル名。
     * <p>
     * 例: {@code "design-book-process.md"}
     * </p>
     */
    private String fileName;

    /**
     * ファイルの種別（拡張子または形式識別子）。
     * <p>
     * 例: {@code "md"}（Markdown）、{@code "docx"} など。
     * API側でファイルタイプに基づく処理ルートが分岐されます。
     * </p>
     */
    private String type;

    /**
     * AIが生成した構造化データのJSON文字列。
     * <p>
     * 実際のAPIレスポンスではこのフィールドは {@code "binaryCode"} というキーで送信されるため、
     * {@link JsonProperty} アノテーションでマッピングしています。
     * 内容はツリー構造やテーブル情報を含む複雑なJSONであり、
     * 後続処理で {@link com.fasterxml.jackson.databind.JsonNode} などにパースして利用します。
     * </p>
     * <p>
     * 注意: このフィールドは「バイナリコード」を意味するものではなく、
     * 「構造化されたテキスト表現（JSON）」を指します。
     * </p>
     */
    @JsonProperty("binaryCode")
    private String binaryCodeJson;

    /**
     * ファイルのステレオタイプ（形式テンプレート識別子）。
     * <p>
     * 設計書の種類やテンプレート名を示す文字列。
     * 例: {@code "design-book-process.md"}。
     * 同じ拡張子でも異なるテンプレート（例: 画面設計 vs バッチ設計）を区別するために使用されます。
     * </p>
     */
    private String stereoType;

    /**
     * 出力先ファイルパス（相対または絶対パス）。
     * <p>
     * 構造化結果を保存・出力する際のファイルパス。
     * 例: {@code "docs/design-book-process.md"}
     * </p>
     */
    private String filePath;

    /**
     * ファイルのカテゴリ情報（現状は未使用、常に {@code null}）。
     * <p>
     * 将来的な拡張用に予約されたフィールド。
     * 現在のAIサポートAPIではこの値は返却されません。
     * </p>
     */
    private Object category; 

    /**
     * 関連付けられたリポジトリ識別子（GitリポジトリIDなど）。
     * <p>
     * ファイルがどのソースコードリポジトリに属するかを示す一意の文字列。
     * 例: {@code "69845ecbbfd02aa4291f2ac8"}
     * </p>
     */
    private String repository;

    /**
     * 関連タスクID（現状は未使用、常に {@code null}）。
     * <p>
     * 将来的にチケット管理システム（例: Jira）との連携を想定したフィールド。
     * 現在のAIサポートAPIではこの値は返却されません。
     * </p>
     */
    private Object taskId;
}