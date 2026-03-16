package com.neusoft.bsdl.wptool.generate.model;

import java.util.function.Function;

public class ItemPropMapping {
    public final String propLabel;
    public final String propKey;
    public final Function<String, String> converter;
    private static final Function<String, String> DEFAULT_CONVERTER = s -> s;

    public ItemPropMapping(String propLabel, String propKey) {
        this(propLabel, propKey, null);
    }

    public ItemPropMapping(String propLabel, String propKey, Function<String, String> converter) {
        this.propLabel = propLabel;
        this.propKey = propKey;
        this.converter = converter;
    }

    public String apply(String input) {
        if (input == null)
            return DEFAULT_CONVERTER.apply(null);
        if (converter != null)
            return converter.apply(input);
        return DEFAULT_CONVERTER.apply(input);
    }
}
