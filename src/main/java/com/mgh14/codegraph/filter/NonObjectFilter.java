package com.mgh14.codegraph.filter;

import org.apache.commons.lang3.StringUtils;

/**
 *
 */
public class NonObjectFilter implements Filter {
    @Override
    public boolean filterOutClass(String className) {
        return StringUtils.isNotBlank(className) && !className.toLowerCase().equals("java.lang.object");
    }
}
