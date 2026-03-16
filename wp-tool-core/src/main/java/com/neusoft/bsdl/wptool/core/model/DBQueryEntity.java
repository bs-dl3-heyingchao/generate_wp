package com.neusoft.bsdl.wptool.core.model;

import lombok.Data;

@Data
public class DBQueryEntity {
	/**項番*/
	private String itemNo;
	/**カラム名:物理名*/
	private String physicalName;
	/**カラム名:論理名*/
	private String logicalName;
	/**NULL可*/
	private Boolean isNullable;
	/**キーグループ*/
	private String keyGroup;
	/**長さ:PRE*/
	private String lengthPre;
	/**長さ:S*/
	private String lengthS;
	/**長さ:B*/
	private String lengthB;
	/**データ型(WP)*/
	private String dataTypeWP;
	/**DB定義:型*/
	private String dbDefineType;
	/**DB定義:桁数*/
	private String dbDefineLength;
	/**DB定義:小数桁*/
	private String dbDefineDecimal;
	/**備考*/
	private String remark;
	/**使用文字種*/
	private String encodeType;
	/**取得元:テーブル名*/
	private String resourceTableName;
	/**取得元:カラム名*/
	private String resourceColumnName;
}
