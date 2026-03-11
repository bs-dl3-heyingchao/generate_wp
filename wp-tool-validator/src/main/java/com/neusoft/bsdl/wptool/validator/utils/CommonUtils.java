package com.neusoft.bsdl.wptool.validator.utils;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public class CommonUtils {
	/**
	 * 
	 * @param input       バリデーション対象文字列
	 * @param validValues バリデーションで必要なタグの配列
	 * @return inputが空の場合はtrueを返し、そうでない場合はvalidValuesのすべてのタグがinputに含まれているかをチェックして結果を返す
	 */
	public static boolean containsAllRequiredTags(String input, String[] validValues) {
		// 空の場合は有効とみなす
		if (StringUtils.isEmpty(input)) {
			return true;
		}
		return Arrays.stream(validValues).allMatch(input::contains);
	}
}
