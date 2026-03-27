package com.neusoft.bsdl.wptool.generate.model;

import lombok.Data;

@Data
public class DmOpeLogic {

    public String item_code;
    public String statement;
    public String description;
    public String is_disable = "false";
}
