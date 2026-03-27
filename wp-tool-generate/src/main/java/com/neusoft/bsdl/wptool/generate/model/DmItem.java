package com.neusoft.bsdl.wptool.generate.model;

import lombok.Data;

@Data
public class DmItem {
	public String code;
	public String name;
	public String length;
	public String byteSize;
	public String scale;
	public String is_nullable;
	public String key_group;
	public String data_type;
	public String is_disable = "false";
}
