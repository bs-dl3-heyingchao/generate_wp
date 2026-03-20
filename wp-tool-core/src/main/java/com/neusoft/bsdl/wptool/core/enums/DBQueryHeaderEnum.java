package com.neusoft.bsdl.wptool.core.enums;

/**
 * DBクエリー定義書のヘッダ列挙
 */
public enum DBQueryHeaderEnum implements HeaderEnum {
	ITEM_NO(0, "項番", 0), COLUMN_NAME(0,"カラム名", 1),
    IS_NULLABLE(0,"NULL可", 3),
    KEY_GROUP(0,"キー\nグループ", 4),
    LENGTH_PRE(1,"PRE", 5),
    LENGTH_S(1,"S", 6),
    LENGTH_B(1,"B", 7),
    DATA_TYPE_WP(0,"データ型(WP)", 9),
    DB_DEFINE_TYPE(1,"型", 10),
    DB_DEFINE_LENGTH(1,"桁数", 11),
    DB_DEFINE_DECIMAL(1,"小数桁", 12),
    REMARK(0,"備考", 15),    
    ENCODE_TYPE(0,"使用文字種", 27);
    //SOURCE_TABLE(1,"テーブル名", 35), 
    //SOURCE_COLUMN(1,"カラム名", 36); 
	
	private final int level;
	private final String displayName;
	private final int columnIndex;

	DBQueryHeaderEnum(int level, String displayName, int columnIndex) {
		this.level = level;
		this.displayName = displayName;
		this.columnIndex = columnIndex;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public int getColumnIndex() {
		return columnIndex;
	}

	public int getLevel() {
		return level;
	}
}
