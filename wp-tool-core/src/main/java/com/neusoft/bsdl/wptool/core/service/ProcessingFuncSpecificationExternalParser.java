package com.neusoft.bsdl.wptool.core.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.neusoft.bsdl.wptool.core.CommonConstant.PROCESSING_FUNCTION_SPECIFICATION_SHEET;
import com.neusoft.bsdl.wptool.core.external.AiSupportApiResponse;
import com.neusoft.bsdl.wptool.core.external.AiSupportApiService;
import com.neusoft.bsdl.wptool.core.external.FunctionalSpecification;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ProcessingFuncSpecification;
import com.neusoft.bsdl.wptool.core.model.ProcessingFuncSpecificationBtnOperation;
import com.neusoft.bsdl.wptool.core.model.ProcessingFuncSpecificationBtnOperationForward;
import com.neusoft.bsdl.wptool.core.model.ProcessingFuncSpecificationBtnOperationMsg;
import com.neusoft.bsdl.wptool.core.model.ProcessingFuncSpecificationParam;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 処理機能仕様書を解析するクラス。
 * AIサポートAPIから取得した構造化JSONを、内部モデル（ProcessingFuncSpecification）に変換します。
 */
@Slf4j
public class ProcessingFuncSpecificationExternalParser {

	/**
	 * 外部ファイル（Excel等）をAIサポートAPIに送信し、処理機能仕様書の構造化データを解析して返却します。
	 * 
	 * @param source 解析対象のファイルソース（LocalFileSourceのみ対応）
	 * @return 解析済みの処理機能仕様書オブジェクト
	 * @throws Exception
	 */
	public ProcessingFuncSpecification exeternalParser(FileSource source, String sheetName) throws Exception {
		AiSupportApiResponse aiSupportApiResponse = null;
		try {
			// 指定されたファイルをAIサポート外部APIに送信し、構造化されたJSONレスポンスを取得します。
			aiSupportApiResponse = AiSupportApiService.callAiSupportApi(source);
		} catch (Exception e) {
			log.error("call remote api failed :{}", e.getMessage());
			// APIアクセス失敗の場合、仕様書から1.パラメータ のコンテンツを読込む
			ProcessingFuncSpecificationParseExcel parseExcel = new ProcessingFuncSpecificationParseExcel();
			ProcessingFuncSpecification contents = parseExcel.parseSpecSheet(source, sheetName);
			return contents;
		}

		List<FunctionalSpecification> targetParseObj = aiSupportApiResponse.getData();

		if (targetParseObj == null || targetParseObj.size() == 0) {
			throw new IllegalStateException("AI API returned empty data");
		}

		// 仕様書の処理機能記述書のJSONコンテンツを取得する
		String binaryCodeJsonStr = aiSupportApiResponse.getData().get(0).getBinaryCodeJson().toString();
		log.info("binaryCodeJsonStr :{}", binaryCodeJsonStr);
		
		if (binaryCodeJsonStr == null || binaryCodeJsonStr.trim().isEmpty()) {
			throw new IllegalStateException("binaryCodeJson is null or empty");
		}

		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode;
		rootNode = mapper.readTree(binaryCodeJsonStr);

		return parseProcessingFuncSpecification(rootNode);
	}

	/**
	 * JSONルートノードから処理機能仕様書全体を解析します。 主に「1.パラメータ」と「3.～ボタン押下処理」セクションを識別・抽出します。
	 * 
	 * @param rootNode AIサポートAPIから取得したJSONのルートノード
	 * @return 解析結果を格納したProcessingFuncSpecificationインスタンス
	 */
	private ProcessingFuncSpecification parseProcessingFuncSpecification(JsonNode rootNode) {
		ProcessingFuncSpecification spec = new ProcessingFuncSpecification();
		List<ProcessingFuncSpecificationParam> parameters = new ArrayList<>();
		List<ProcessingFuncSpecificationBtnOperation> btnOperations = new ArrayList<>();

		JsonNode childTreeNodes = rootNode.get(PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_NODE_CHILD_TREE_NODES);
		if (childTreeNodes == null || !childTreeNodes.isArray()) {
			log.warn("childTreeNodes not found or not an array");
			spec.setParams(parameters);
			spec.setBtnOpertions(btnOperations);
			return spec;
		}

		for (JsonNode node : childTreeNodes) {
			String nodeKey = safeText(node, PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_NODE_NODE_KEY);
			if (PROCESSING_FUNCTION_SPECIFICATION_SHEET.SECTION_PARAM.equals(nodeKey)) {
				// 1.パラメータ
				parameters.addAll(parseParameters(node));
			} else if (nodeKey != null
					&& nodeKey.contains(PROCESSING_FUNCTION_SPECIFICATION_SHEET.SECTION_BTN_OPERATION)) {
				// ボタン押下処理を含む処理セクション
				List<ProcessingFuncSpecificationBtnOperation> ops = parseButtonOperation(nodeKey, node);
				if (!ops.isEmpty()) {
					btnOperations.addAll(ops);
				}
			}
		}
		spec.setParams(parameters);
		spec.setBtnOpertions(btnOperations);
		return spec;
	}

	/**
	 * 「1.パラメータ」セクションのvalueノードから、パラメータテーブル（tableList）を抽出します。 動的なキー名（例:
	 * table_4_7_3）を意識せずに、再帰的にtableListを持つオブジェクトを探します。
	 * 
	 * @param paramNode 「1.パラメータ」ノード
	 * @return 抽出されたパラメータリスト
	 */
	// === 1. パラメータ解析 ===
	private List<ProcessingFuncSpecificationParam> parseParameters(JsonNode paramNode) {
		JsonNode value = paramNode.get(PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_NODE_VALUE);
		if (value == null || !value.isObject()) {
			return Collections.emptyList();
		}
		for (JsonNode fieldValue : value) {
			if (fieldValue.isObject()) {
				JsonNode tableList = fieldValue.get(PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_NODE_TABLE_LIST);
				if (tableList != null && tableList.isArray()) {
					List<ProcessingFuncSpecificationParam> params = new ArrayList<>();
					for (JsonNode row : tableList) {
						ProcessingFuncSpecificationParam p = new ProcessingFuncSpecificationParam();
						p.setSort(safeText(row, PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_COL_SORT));
						p.setLogicName(safeText(row, PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_COL_ITEM));
						params.add(p);
					}
					return params;
				}
			}
		}
		return Collections.emptyList();
	}

	/**
	 * ボタン押下処理（例: 「更新」ボタン）の子ノードを解析し、 メッセージ一覧と次画面遷移情報を抽出します。
	 * 
	 * @param nodeKey    ボタン処理の識別名（例: "3.「更新」ボタン押下処理"）
	 * @param buttonNode ボタン処理ノード全体
	 * @return 抽出されたボタン操作情報（通常1件）
	 */
	// === ボタン操作解析（如「更新」ボタン）===
	private List<ProcessingFuncSpecificationBtnOperation> parseButtonOperation(String nodeKey, JsonNode buttonNode) {
		List<ProcessingFuncSpecificationBtnOperation> operations = new ArrayList<>();
		JsonNode children = buttonNode.get(PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_NODE_CHILD_TREE_NODES);
		if (children == null || !children.isArray()) {
			return operations;
		}

		ProcessingFuncSpecificationBtnOperation operation = new ProcessingFuncSpecificationBtnOperation();

		for (JsonNode child : children) {
			String childNodeKey = safeText(child, PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_NODE_NODE_KEY);
			JsonNode value = child.get(PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_NODE_VALUE);
			if (PROCESSING_FUNCTION_SPECIFICATION_SHEET.SECTION_SUB_MESSAGE.equals(childNodeKey) && value != null) {
				// 1)メッセージ
				operation.setMessages(parseMessages(value));
			} else if (PROCESSING_FUNCTION_SPECIFICATION_SHEET.SECTION_SUB_REDIREECT.equals(childNodeKey)
					&& value != null) {
				// 2)次画面遷移
				operation.setScreenFoward(parseScreenForward(value));
			}
		}
		operation.setOpertionName(nodeKey);
		operations.add(operation);
		return operations;
	}

	/**
	 * メッセージセクションのvalueから、事前/OK/NGメッセージを抽出します。 メッセージ形式：「事前メッセージ：MZZG0003
	 * 更新してもよろしいですか？」
	 * 
	 * @param value メッセージセクションのvalueノード（テキスト値の集合）
	 * @return 抽出されたメッセージリスト（最大3件）
	 */
	private List<ProcessingFuncSpecificationBtnOperationMsg> parseMessages(JsonNode value) {
		List<ProcessingFuncSpecificationBtnOperationMsg> messages = new ArrayList<>();
		for (JsonNode fieldValue : value) {
			if (!fieldValue.isString())
				continue;
			String line = fieldValue.asString().trim();

			if (line.startsWith(PROCESSING_FUNCTION_SPECIFICATION_SHEET.JIZEN_MESSAGE_ITEM)) {
				messages.add(buildMessage(PROCESSING_FUNCTION_SPECIFICATION_SHEET.JIZEN_MESSAGE_ITEM, line));
			} else if (line.startsWith(PROCESSING_FUNCTION_SPECIFICATION_SHEET.OK_MESSAGE_ITEM)) {
				messages.add(buildMessage(PROCESSING_FUNCTION_SPECIFICATION_SHEET.OK_MESSAGE_ITEM, line));
			} else if (line.startsWith(PROCESSING_FUNCTION_SPECIFICATION_SHEET.NG_MESSAGE_ITEM)) {
				messages.add(buildMessage(PROCESSING_FUNCTION_SPECIFICATION_SHEET.NG_MESSAGE_ITEM, line));
			}
		}
		return messages;
	}

	/**
	 * 1行のメッセージテキストから、メッセージ名・ID・内容を分解してオブジェクト化します。
	 * 正規表現で「：」以降のIDと内容を分割試行し、失敗時は全文を内容として扱います。
	 * 
	 * @param name     メッセージ種別（例: "事前メッセージ"）
	 * @param fullLine 元テキスト（例: "事前メッセージ：MZZG0003 更新してもよろしいですか？"）
	 * @return 構造化されたメッセージオブジェクト
	 */
	private ProcessingFuncSpecificationBtnOperationMsg buildMessage(String name, String fullLine) {
		ProcessingFuncSpecificationBtnOperationMsg msg = new ProcessingFuncSpecificationBtnOperationMsg();
		msg.setMessageName(name);

		Pattern pattern = Pattern.compile(PROCESSING_FUNCTION_SPECIFICATION_SHEET.PATTERN_MESSAGE);
		Matcher matcher = pattern.matcher(fullLine.substring(name.length()).trim());

		if (matcher.find()) {
			msg.setMessageId(matcher.group(1).trim());
			msg.setMessageContents(matcher.group(2).trim());
		} else {
			int colonIndex = fullLine.indexOf(PROCESSING_FUNCTION_SPECIFICATION_SHEET.MESSAGE_COLON);
			if (colonIndex != -1) {
				String rest = fullLine.substring(colonIndex + 1).trim();
				msg.setMessageContents(rest);
			} else {
				msg.setMessageContents(fullLine);
			}
		}
		return msg;
	}

	/**
	 * 「2)次画面遷移」セクションのvalueを解析し、 ・変更破棄確認 ・次入出力 ・次入出力のパラメータ（tableList） を抽出します。
	 * 動的キー（例: table_4_25_5）への依存を避けるため、再帰検索を使用。
	 * 
	 * @param value 次画面遷移セクションのvalueノード
	 * @return 構造化された次画面遷移情報
	 */
	private ProcessingFuncSpecificationBtnOperationForward parseScreenForward(JsonNode value) {
		ProcessingFuncSpecificationBtnOperationForward forward = new ProcessingFuncSpecificationBtnOperationForward();

		if (value == null || !value.isObject()) {
			forward.setInOutParams(Collections.emptyList());
			return forward;
		}

		List<String> textualValues = new ArrayList<>();
		for (JsonNode child : value) {
			if (child.isString()) {
				textualValues.add(child.asString().trim());
			}
		}

		for (String line : textualValues) {
			if (line.startsWith(PROCESSING_FUNCTION_SPECIFICATION_SHEET.MODIFY_CONFIRM)) {
				// 変更破棄確認のコンテンツを解析する
				forward.setModifyConfirm(extractAfterColon(line));
			} else if (line.startsWith(PROCESSING_FUNCTION_SPECIFICATION_SHEET.IN_OUT)) {
				// 次入出力のコンテンツを解析する
				forward.setInOut(extractAfterColon(line));
			}
		}

		List<ProcessingFuncSpecificationParam> inOutParams = extractTableListFromNode(value);
		forward.setInOutParams(inOutParams);

		return forward;
	}

	/**
	 * 「XXX：YYY」形式の文字列から「：」以降の部分を抽出します。
	 * 
	 * @param line 元文字列
	 * @return 「：」以降の内容（存在しない場合は空文字）
	 */
	private String extractAfterColon(String line) {
		int idx = line.indexOf("：");
		return idx != -1 ? line.substring(idx + 1).trim() : "";
	}

	/**
	 * 指定フィールドが存在する場合、そのテキスト値を取得します。 存在しない場合は空文字を返却し、NullPointerExceptionを回避します。
	 * 
	 * @param node  対象JsonNode
	 * @param field 取得したいフィールド名
	 * @return フィールド値（存在しない場合は空文字）
	 */
	private String safeText(JsonNode node, String field) {
		return node.has(field) ? node.get(field).asString() : "";
	}

	/**
	 * 指定ノード（およびその子孫）から、tableListを持つオブジェクトを再帰的に探索し、 パラメータリストを抽出します。 動的キー（例:
	 * table_4_25_5）に依存せず、構造ベースで検出可能です。
	 * 
	 * @param node 探索開始ノード
	 * @return 抽出されたパラメータリスト（見つからない場合は空リスト）
	 */
	private List<ProcessingFuncSpecificationParam> extractTableListFromNode(JsonNode node) {
		if (node == null || !node.isObject()) {
			return Collections.emptyList();
		}

		JsonNode tableList = node.get(PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_NODE_TABLE_LIST);
		if (tableList != null && tableList.isArray()) {
			List<ProcessingFuncSpecificationParam> params = new ArrayList<>();
			for (JsonNode row : tableList) {
				ProcessingFuncSpecificationParam p = new ProcessingFuncSpecificationParam();
				p.setSort(safeText(row, PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_COL_SORT));
				p.setLogicName(safeText(row, PROCESSING_FUNCTION_SPECIFICATION_SHEET.STR_COL_ITEM));
				params.add(p);
			}
			return params;
		}

		for (JsonNode child : node) {
			if (child.isObject() || child.isArray()) {
				List<ProcessingFuncSpecificationParam> found = extractTableListFromNode(child);
				if (!found.isEmpty()) {
					return found;
				}
			}
		}
		return Collections.emptyList();
	}
}