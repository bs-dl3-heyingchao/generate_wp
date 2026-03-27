package com.neusoft.bsdl.wptool.generate.model;

import java.util.List;

import lombok.Data;

@Data
public class DmOpe {

    public String code;
    public String name;
    public String type;
    public String is_commit = "false";
    public String is_disable = "false";
    public List<DmOpeLogic> logicList;
}
