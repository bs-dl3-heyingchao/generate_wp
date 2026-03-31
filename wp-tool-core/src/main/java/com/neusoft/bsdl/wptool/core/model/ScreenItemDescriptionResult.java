package com.neusoft.bsdl.wptool.core.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 画面項目説明書の返却結果
 */
@Data
@NoArgsConstructor
public class ScreenItemDescriptionResult implements Serializable {
	private static final long serialVersionUID = 1L;
	private String groupName;
	private List<ScreenItemDescription> items;
}