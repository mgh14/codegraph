package com.mgh14.codegraph.filter;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 *
 */
public class CompositeFilter implements Filter {

    private final Filter[] filters;

    private CompositeFilter(Filter... filters) {
        this.filters = filters;
    }

    @Override
    public boolean filterOutClass(String className) {
        return Arrays.stream(filters).anyMatch(filter -> filter.filterOutClass(className));
    }

    public static Filter ofFilters(Filter... filters) {
        return new CompositeFilter(filters);
    }
}
