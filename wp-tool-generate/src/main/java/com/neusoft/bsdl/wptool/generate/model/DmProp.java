package com.neusoft.bsdl.wptool.generate.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DmProp {
    public String key;
    public String value;
    public String is_disable = "false";
}
