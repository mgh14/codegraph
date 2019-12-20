package com.mgh14.codegraph.printtracer;

import lombok.Value;

/**
 * TODO: Document
 */
@Value
public class PrintMethodReference {

    int opcode;
    String name;
    String desc;
    boolean isInterface;
}
