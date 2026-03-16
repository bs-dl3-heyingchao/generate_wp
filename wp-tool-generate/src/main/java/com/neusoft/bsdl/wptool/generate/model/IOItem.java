package com.neusoft.bsdl.wptool.generate.model;

import java.util.List;

import lombok.Data;

@Data
public class IOItem {
	public String io_code;
	public String code;
	public String name;
	public String label;
	public String is_visible = "true";
	public String item_type = "IO";
	public String is_require = "false";
	public String length = "0";
	public String scale = "-1";
	public String min_length = "0";
	public String max_length = "0";
	public String max_ByteSize = "0";
	public String format;
	public String level = "1";
	public String sort_key = "0";
	public String sort_type;
	public String dm_code;
	public String dm_item_code;
	public String is_disable = "false";
	public String creator = "eclipse";;
	public String create_time = "2025-04-01 00:00:00.0";
	public String default_value;
	public String statement;
	public String condition;
	public String description;
	public String msg_code_ng;
	public String msg_param_ng;
	public ChoiceBean choiceInfo;
	public List<ItemProp> io_item_prop_list;
//	public String io_item_ext_prop_list;
//	public String io_item_next_io_logic_list;
}
