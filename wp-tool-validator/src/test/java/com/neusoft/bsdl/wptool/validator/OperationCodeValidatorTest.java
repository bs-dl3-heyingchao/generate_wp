package com.neusoft.bsdl.wptool.validator;

public class OperationCodeValidatorTest {

	
	public static void main(String[] args) {
		// 登录（登録）
		System.out.println(isValidCode("MTI350S01_I010", "登録")); // true
		System.out.println(isValidCode("MTI350S01_U010", "登録")); // false ❌

		// 更新（更新）
		System.out.println(isValidCode("MTI350S01_U020", "更新")); // true
		System.out.println(isValidCode("MTI350S01_I020", "更新")); // false ❌

		// 格式错误
		System.out.println(isValidCode("MTI35S01_I010", "登録")); // false（前段不是9位）
		System.out.println(isValidCode("MTI350S01_I01", "登録")); // false（后段不是4位）
		System.out.println(isValidCode("mti350s01_i010", "登録")); // false（含小写）
		
		System.out.println("-----------------------------"); // false
		System.out.println(isValidName("User_登録")); // false
		System.out.println(isValidName("123_登録")); // false
		System.out.println(isValidName("登録 登録")); // false
		System.out.println(isValidName("汎用テーブル詳細_登録")); // true
		
	}


	/**
	 * 校验操作名称（名前）
	 */
	public static boolean isValidName(String name) {
		return name != null && !name.isEmpty() && CommonConstant.DB_CONFIG_SHEET.PATTERN_NAME.matcher(name).matches();
	}

	/**
	 * 校验操作コード
	 * 
	 * @param code      操作コード，如 "MTI350S01_I010"
	 * @param operation 操作类型："登録" 或 "更新"
	 * @return 是否合法
	 */
	public static boolean isValidCode(String code, String operation) {
		if (code == null || operation == null) {
			return false;
		}

		var matcher = CommonConstant.DB_CONFIG_SHEET.PATTERN_OPERATION_CODE.matcher(code);
		if (!matcher.matches()) {
			return false; // 结构不符合
		}

		String prefixType = matcher.group(1); // "I" 或 "U"

		if (CommonConstant.DB_CONFIG_SHEET.STR_DB_OPERATION_INSERT_NAME.equals(operation)) {
			return CommonConstant.DB_CONFIG_SHEET.STR_DB_OPERATION_INSERT_PREFIX.equals(prefixType);
		} else if (CommonConstant.DB_CONFIG_SHEET.STR_DB_OPERATION_UPD_NAME.equals(operation)) {
			return CommonConstant.DB_CONFIG_SHEET.STR_DB_OPERATION_UPD_PREFIX.equals(prefixType);
		} else {
			return false; 
		}
	}
}