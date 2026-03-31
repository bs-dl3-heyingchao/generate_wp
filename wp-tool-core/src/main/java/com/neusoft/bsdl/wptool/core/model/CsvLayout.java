package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * CSVレイアウトの解析内容
 */
@Data
public class CsvLayout implements Serializable {
    private static final long serialVersionUID = 1L;
	//機能名称
	private String functionName;
	//ファイルID
	private String fileId;
	//ファイル名
	private String fileName;
	//入出力種別
	private String inputOutputType;
	//ファイル形式
	private String fileFormat;
	//ファイル名規則
	private String fileNamingRule;
	//文字コード
	private String characterEncoding;
	//改行コード
	private String lineEncoding;
	//特記事項
	private String specialNotes;
	//一覧情報
	private List<CsvSubLayout> csvSubLayouts;
}