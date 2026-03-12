package com.neusoft.bsdl.wptool.core.service;


import com.neusoft.bsdl.wptool.core.model.MessageDefinition;

/**
 * テーブル/フィールド定義を名前で検索するサービス。
 */
public interface IWPMessageLoaderService {

	/**
     * 消息IDからメッセージ定義を取得します。
     * @param messageId メッセージID（例: "MCMG0101"）
     * @return 該当するメッセージ定義。存在しない場合は {@code null}
     */
    MessageDefinition findMessageById(String messageId);
}