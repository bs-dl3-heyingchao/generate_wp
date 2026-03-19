package com.neusoft.bsdl.wptool.core.external;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.service.ConfigService;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * AIサポート外部APIと連携し、設計書などの構造化JSONデータを取得するサービスクラス。
 * <p>
 * 指定されたファイルをmultipart/form-data形式でAIサポートAPIに送信し、
 * 応答として返却された{@link AiSupportApiResponse}を解析・整形して返却します。
 * 特に、応答JSON内に含まれるテキスト値中のタブ文字（\t）を4つの半角スペースに置換し、 視認性と整形性を向上させます。
 * </p>
 */
public class AiSupportApiService {

	/** 外部AIサポートAPIのエンドポイントURL。設定ファイルから読み込みます。 */
	public static final String URL = ConfigService.getConfig("wp-tool.ai.support.url");

	/** 外部AIサポートAPIアクセス用のBearerトークン。設定ファイルから読み込みます。 */
	public static final String TOKEN = ConfigService.getConfig("wp-tool.ai.support.token");
	/**
	 * 共通で使用する{@link ObjectMapper}インスタンス。
	 * <p>
	 * 次の設定を適用しています：
	 * <ul>
	 * <li>未知のプロパティを無視してエラーとしない（{@code FAIL_ON_UNKNOWN_PROPERTIES = false}）</li>
	 * </ul>
	 * このインスタンスは不変（immutable）かつスレッドセーフです。アプリケーション全体で再利用可能です。
	 * </p>
	 */
	public static final JsonMapper MAPPER = new JsonMapper().rebuild().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

	/**
	 * 指定されたファイルをAIサポート外部APIに送信し、構造化されたJSONレスポンスを取得します。
	 * <p>
	 * ファイルはローカルパスから読み込まれ、multipart/form-data形式でPOSTされます。
	 * APIからの応答JSONは{@link AiSupportApiResponse}オブジェクトにマッピングされ、
	 * その中の{@code binaryCodeJson}フィールドに含まれるタブ文字（\t）を4つの半角スペースに置換します。
	 * </p>
	 *
	 * @param fileName 処理対象のローカルファイルパス（例: "design-book-process.md"）
	 * @return AIサポートAPIからの構造化応答データをラップした{@link AiSupportApiResponse}オブジェクト
	 * @throws Exception ファイル読み込み、HTTP通信、JSONパースなど何らかのエラーが発生した場合
	 */
	public static AiSupportApiResponse callAiSupportApi(FileSource source) throws Exception {
		// ローカルファイルを読み込む
		byte[] fileBytes = source.getInputStream().readAllBytes();

		// multipart/form-data の境界文字（boundary）を生成
		String boundary = "----Boundary" + UUID.randomUUID().toString();
		String fileName = source instanceof LocalFileSource ? ((LocalFileSource) source).getFileName() : "file";
		byte[] requestBody = buildMultipartBody(boundary, "file", fileName, fileBytes);

		// HTTPクライアントを構築（接続タイムアウト：10秒）
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

		// POSTリクエストを構築
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(URL)).header("Authorization", "Bearer " + TOKEN)
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.POST(HttpRequest.BodyPublishers.ofByteArray(requestBody)).timeout(Duration.ofSeconds(30)) // リクエスト全体のタイムアウト
				.build();

		// 外部APIを呼び出し
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		// JSONレスポンスをオブジェクトに変換
		AiSupportApiResponse apiResponse = MAPPER.readValue(response.body(), AiSupportApiResponse.class);

		// 応答データ内の binaryCodeJson を整形：タブ文字（\t）を4つの半角スペースに置換
		apiResponse.getData().forEach(fileData -> {
			String escapedJson = fileData.getBinaryCodeJson();
			try {
				// JSON文字列をJsonNodeにパース
				JsonNode node = MAPPER.readTree(escapedJson);
				// JsonNodeを整形済みJSON文字列に変換し、\\t（JSONエスケープされたタブ）をスペースに置換
				// 注意: writeValueAsString() により、元の\tはJSON内で "\\t" として出力されるため、
				// "\\t"（2文字：バックスラッシュ + t）を置換対象とする
				String prettyJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node).replace("\\t",
						"    ");
				fileData.setBinaryCodeJson(prettyJson);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return apiResponse;
	}

	/**
	 * multipart/form-dataリクエストボディを構築します。
	 * <p>
	 * 指定されたファイルデータを、RFC準拠のmultipartフォーマットにエンコードします。
	 * </p>
	 *
	 * @param boundary  multipartの境界文字（例: "----Boundary12345..."）
	 * @param fieldName フォームフィールド名（通常は "file"）
	 * @param fileName  アップロードするファイル名（Content-Dispositionに使用）
	 * @param fileData  アップロードするファイルのバイナリデータ
	 * @return 構築されたmultipart/form-dataリクエストボディ（UTF-8エンコード済み）
	 */
	private static byte[] buildMultipartBody(String boundary, String fieldName, String fileName, byte[] fileData) {
		String crlf = "\r\n";
		StringBuilder sb = new StringBuilder();
		// パートヘッダーを構築
		sb.append("--").append(boundary).append(crlf);
		sb.append("Content-Disposition: form-data; name=\"").append(fieldName).append("\"; filename=\"")
				.append(fileName.replace("\"", "\\\"")) // ファイル名にダブルクォートが含まれる場合のエスケープ
				.append("\"").append(crlf);
		sb.append("Content-Type: application/octet-stream").append(crlf);
		sb.append(crlf); // ヘッダーとボディの間の空行

		String header = sb.toString();
		String footer = crlf + "--" + boundary + "--" + crlf;

		// ヘッダー、ファイルデータ、フッターを結合
		byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
		byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
		byte[] result = new byte[headerBytes.length + fileData.length + footerBytes.length];

		System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
		System.arraycopy(fileData, 0, result, headerBytes.length, fileData.length);
		System.arraycopy(footerBytes, 0, result, headerBytes.length + fileData.length, footerBytes.length);

		return result;
	}
}