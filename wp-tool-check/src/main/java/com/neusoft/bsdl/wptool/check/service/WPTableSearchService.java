package com.neusoft.bsdl.wptool.check.service;

import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;

/**
 * テーブル/フィールド定義を名前で検索するサービス。
 */
public interface WPTableSearchService {

    /**
     * 論理名からテーブル定義を検索する。
     *
     * @param tableFullName テーブルの論理名
     * @return 該当するテーブル定義。見つからない場合はnull
     */
    TableBean findTableByFullName(String tableFullName);

    /**
     * テーブル名からテーブル定義を検索する。
     *
     * @param tableName テーブル名
     * @return 該当するテーブル定義。見つからない場合はnull
     */
    TableBean findTableByName(String tableName);

    /**
     * テーブル論理名とフィールド論理名からフィールド定義を検索する。
     *
     * @param tableFullName テーブルの論理名
     * @param fieldFullName フィールドの論理名
     * @return 該当するフィールド定義。見つからない場合はnull
     */
    FieldBean findFieldByFullName(String tableFullName, String fieldFullName);

    /**
     * テーブル名とフィールド名からフィールド定義を検索する。
     *
     * @param tableName テーブル名
     * @param fieldName フィールド名
     * @return 該当するフィールド定義。見つからない場合はnull
     */
    FieldBean findFieldByName(String tableName, String fieldName);
}