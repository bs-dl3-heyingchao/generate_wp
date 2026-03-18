package com.neusoft.bsdl.wptool.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.StringUtils;
import com.neusoft.bsdl.wptool.core.CommonConstant.SESSION_MANAGEMENT_SHEET;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.SessionManagementContent;
import com.neusoft.bsdl.wptool.core.model.SessionManagementSystemField;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionManagementParseExcel extends AbstractParseTool {

	/**
	 * 共通仕様書の「セッション管理」シートを解析し、システム項目一覧を取得する。
	 * 
	 * @param source    Excelファイルソース
	 * @param sheetName 解析対象シート名
	 * @return システム項目内容
	 * @throws Exception 入出力エラー等
	 */
	public SessionManagementContent parseSpecSheet(FileSource source, String sheetName) throws Exception {
		Map<String, List<SessionManagementSystemField>> sessionManagement = new HashMap<>();
		List<SessionManagementSystemField> systemFields = new ArrayList<>();

		EasyExcel.read(source.getInputStream(), SessionManagementSystemField.class,
				new AnalysisEventListener<SessionManagementSystemField>() {
					// 「分類」列が「システム項目」であるブロック内かどうかを判定するフラグ
					boolean inSystemType = false;
					// 現在のセクション名（例：■ログインユーザ情報）
					String sectionName = null;

					@Override
					public void invoke(SessionManagementSystemField data, AnalysisContext context) {
						// データ行がnullの場合、スキップ
						if (data == null)
							return;

						// A列：分類
						String category = data.getCategory();
						// B列：セッションキー
						String sessionKey = data.getSessionKey();

						// 完全な空行（分類もセッションキーもなし）はスキップ
						if (category == null && (StringUtils.isEmpty(sessionKey))) {
							return;
						}

						// 分類列が存在する場合（ブロック開始または他の分類に移行）
						if (category != null) {
							if (SESSION_MANAGEMENT_SHEET.STR_TYPE_SYSTEM_FIELD.equals(category)) {
								// 「システム項目」ブロックに入ったことをマーク
								inSystemType = true;
								SessionManagementSystemField field = new SessionManagementSystemField();
								field.setCategory(SESSION_MANAGEMENT_SHEET.STR_TYPE_SYSTEM_FIELD);
								field.setSessionKey(sessionKey);
								field.setSessionLogicName(data.getSessionLogicName());
								systemFields.add(field);
								return; // 分類行自体は結果に含めない
							} else {
								// 他の分類（例：WebPerformer項目）に移行 → ブロック終了
								inSystemType = false;
								sectionName = null; // セクション名をリセット
								return;
							}
						}

						// 「システム項目」ブロック内で、かつセッションキーが存在する行のみ処理
						if (inSystemType && !StringUtils.isEmpty(sessionKey)) {
							SessionManagementSystemField field = new SessionManagementSystemField();
							// 分類は統一して「システム項目」とする
							field.setCategory(SESSION_MANAGEMENT_SHEET.STR_TYPE_SYSTEM_FIELD);
							field.setSessionKey(sessionKey);
							field.setSessionLogicName(data.getSessionLogicName());

							// ■ログインユーザ情報 などのセクション名を設定
							if (sessionKey.startsWith(SESSION_MANAGEMENT_SHEET.STR_SIKAKU)) {
								// 黄色タイトル行 → セクション名として保存
								sectionName = sessionKey;
							} else {
								// 普通のフィールド行 → 前のセクション名を設定
								field.setSectionName(sectionName);
							}
							systemFields.add(field);
						}
					}

					@Override
					public void doAfterAllAnalysed(AnalysisContext context) {
						log.info("【セッション管理】解析完成，共提取 {} 条システム項目", systemFields.size());
					}
				}).sheet(sheetName).headRowNumber(9).doRead();
		
		sessionManagement.put(sheetName, systemFields);
		
		return new SessionManagementContent(sessionManagement);
	}
}