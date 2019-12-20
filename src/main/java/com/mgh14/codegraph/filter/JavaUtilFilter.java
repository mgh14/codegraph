package com.mgh14.codegraph.filter;

import java.util.Objects;

/**
 *
 */
public class JavaUtilFilter implements Filter {

    @Override
    public boolean filterOutClass(String className) {
        return Objects.isNull(className) || className.toLowerCase().startsWith("java.util");
    }
}
