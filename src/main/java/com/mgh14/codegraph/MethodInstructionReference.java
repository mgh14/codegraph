package com.mgh14.codegraph;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MethodInstructionReference {
    MethodReference parentRef;
    int opcode;
    String name;
    String owner;
    String desc;
    boolean isInterface;
}
