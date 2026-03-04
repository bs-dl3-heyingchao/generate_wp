package com.neusoft.bsdl.wptool.core.model;

import java.util.List;

import lombok.Data;

/**
 * 画面項目説明書の返却結果
 */
@Data
public class ScreenItemDescriptionResult{
	private String groupName;
	private List<ScreenItemDescription> items;
}