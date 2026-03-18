package com.neusoft.bsdl.wptool.core.external;

import java.util.List;

import lombok.Data;

/**
 * AIサポート外部APIからのレスポンスを表現するデータクラス。
 * <p>
 * このクラスは、AIが解析・構造化した設計書などの処理結果をクライアントに返却するための共通フォーマットです。
 * レスポンスステータス、メッセージ、実際のデータ（{@link FileData}リスト）、およびタイムスタンプを含みます。
 * </p>
 */
@Data
public class AiSupportApiResponse {

    /**
     * 処理結果のステータスコード。
     * <p>
     * 通常、"0" は成功を示し、非ゼロ値はエラーまたは警告を示します。
     * 具体的なコード定義はAIサポートAPIの仕様に準拠します。
     * </p>
     */
    private String code;

    /**
     * 処理結果に関する説明メッセージ。
     * <p>
     * 成功時は "success"、エラー時はエラー内容を示す文字列が含まれます。
     * ログ出力やデバッグ用途に使用されます。
     * </p>
     */
    private String message;

    /**
     * 構造化されたファイルデータのリスト。
     * <p>
     * 各 {@link FunctionalSpecification} オブジェクトは、入力ファイル（例: Markdown設計書）から抽出・変換された
     * 構造化JSONデータ（{@code binaryCodeJson}）やメタ情報を保持します。
     * 複数ファイルを一度に処理可能な場合、複数の要素が含まれます。
     * </p>
     */
    private List<FunctionalSpecification> data;

    /**
     * レスポンス生成時刻（Unixエポック時間、ミリ秒単位）。
     * <p>
     * API側でレスポンスが生成された正確な時刻を記録し、
     * クライアント側での処理遅延分析やログ相関に利用できます。
     * </p>
     */
    private long timestamp;
}