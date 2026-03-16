package com.neusoft.bsdl.wptool.validator;

import com.neusoft.bsdl.wptool.validator.CommonConstant.SCREEN_DEFINITION_SHEET;

public class ConditionValidatorTest {
	
    public static void main(String[] args) {
        // ✅ 正确 CASE
        System.out.println(isValidCondition("A = B")); // true
        System.out.println(isValidCondition("A = B AND \nC = D")); // true
        System.out.println(isValidCondition("@NAMEDPARAM\nA = B")); // true
        System.out.println(isValidCondition("@NAMEDPARAM\nA = B,\nC = D")); // true
        System.out.println(isValidCondition("A = B OR \nC = D")); // true

        // ❌ 错误 CASE（结尾非法）
        System.out.println(isValidCondition("A = B,")); // false
        System.out.println(isValidCondition("A = B AND")); // false
        System.out.println(isValidCondition("A = B OR")); // false
        System.out.println(isValidCondition("@NAMEDPARAM\nA = B,")); // false
        System.out.println(isValidCondition("@NAMEDPARAM\nA = B AND")); // false

        // ❌ 错误 CASE（内容非法）
        System.out.println(isValidCondition("@NAMEDPARAM\nA = B AND C = D")); // false
        System.out.println(isValidCondition("A = B, C = D")); // false
    }
    
   
    public static boolean isValidCondition(String condition) {
    	String trimmed = condition.trim();

		// 通用ルール：行末はコンマ（,）、AND、OR で終わってはならない（末尾の空白を無視）
		if (SCREEN_DEFINITION_SHEET.INVALID_ENDING.matcher(trimmed).matches()) {
			return false;
		}

		boolean hasNamedParam = trimmed.startsWith("@NAMEDPARAM");

		if (hasNamedParam) {
			// @NAMEDPARAM プレフィックスをクリアする
			String content = trimmed.substring("@NAMEDPARAM".length()).trim();

			// AND または OR は任意の位置に出現してはならない
			if (SCREEN_DEFINITION_SHEET.AND_OR_ANYWHERE.matcher(content).find()) {
				return false;
			}

			return true;
		} else {
			// コンマの使用は禁止です
			if (condition.contains(",")) {
				return false;
			}

			return true;
		}
    }
}