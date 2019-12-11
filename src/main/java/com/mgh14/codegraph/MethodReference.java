package com.mgh14.codegraph;

/**
 * TODO: Document
 */
public class MethodReference {

    private final int opcode;
    private final String name;
    private final String desc;
    private final boolean itf;
    private final Object[] args;

    public MethodReference(final int opcode, final String name, final String desc,
                           final boolean itf) {
        this(opcode, name, desc, itf, new Object[]{});
    }

    public MethodReference(final int opcode, final String name, final String desc,
                           final boolean itf, Object... args) {
        this.opcode = opcode;
        this.name = name;
        this.desc = desc;
        this.itf = itf;
        this.args = args;
    }

    public int getOpcode() {
        return opcode;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isItf() {
        return itf;
    }

    public Object[] getArgs() {
        return args;
    }
}