package com.mgh14.codegraph.tracer;

import com.mgh14.codegraph.adapter.javap.dto.MethodCall;
import com.mgh14.codegraph.util.ClassUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.objectweb.asm.Opcodes.ASM5;

public class MethodVisitorPrinter extends MethodVisitor {

  private static final PrintStream printStream = System.out;
  private static final String END_DELIMITER = " //]";

  private final Map<String, List<MethodCall>> allVisitedMethods;
  private int currentVisitCodeCalls = 0;

  public MethodVisitorPrinter() {
    this(ASM5, new HashMap<>());
  }

  private MethodVisitorPrinter(
      final int api, final Map<String, List<MethodCall>> allVisitedMethods) {
    super(api);
    this.allVisitedMethods = allVisitedMethods;
  }

  public Map<String, List<MethodCall>> getVisitedMethods() {
    return allVisitedMethods;
  }

  private String getInitialSpace() {
    return IntStream.range(0, currentVisitCodeCalls)
        .mapToObj(i -> "\t")
        .collect(Collectors.joining());
  }

  private void print(String s) {
    printStream.println(getInitialSpace() + s + END_DELIMITER);
  }

  @Override
  public void visitCode() {
    print("VISIT_CODE: Beginning visit to method...");
    currentVisitCodeCalls++;
    super.visitCode();
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    printInsnVisit("VISIT_METHOD_INSN", opcode, owner, name, desc);
    if (!allVisitedMethods.containsKey(owner)) {
      allVisitedMethods.put(owner, new ArrayList<>());
    }
    allVisitedMethods.get(owner).add(new MethodCall(opcode, name, desc, itf));
  }

  @Override
  public void visitInsn(int opcode) {
    print(String.format("VISIT_INSN: opcode: %s", ClassUtils.identifyOpcode(opcode)));
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    printInsnVisit("VISIT_FIELD_INSN", opcode, owner, name, desc);
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    print(
        String.format(
            "VISIT_VAR_INSN: opcode: %s; var: %s", ClassUtils.identifyOpcode(opcode), var));
  }

  @Override
  public void visitLocalVariable(
      String name, String desc, String signature, Label start, Label end, int index) {
    print(
        String.format(
            "Local variable: name: %s; desc: %s; signature: %s; start: %s; end: %s; index: %d",
            name, desc, signature, start, end, index));
  }

  @Override
  public void visitEnd() {
    print("END_VISIT: visit of method ended");
    currentVisitCodeCalls--;
    super.visitEnd();
  }

  // todo: not sure when this method is called...?
  @Override
  public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
    print(
        String.format(
            "Visiting frame: %s; nlocal: %s; localSize: %d; nStack: %d; stackSize: %d",
            type, nLocal, local.length, nStack, stack.length));
    printLocalVars(local);
    printStack(stack);
    super.visitFrame(type, nLocal, local, nStack, stack);
  }

  private void printInsnVisit(
      final String prefix,
      final int opcode,
      final String owner,
      final String name,
      final String desc) {
    print(
        String.format(
            "%s: name: %s; Opcode: %s; owner: %s; desc: %s ",
            prefix, name, ClassUtils.identifyOpcode(opcode), owner, desc));
  }

  private void printLocalVars(final Object[] localVars) {
    java.util.Arrays.stream(localVars, 0, localVars.length)
        .forEach(obj -> print(String.format("Local var: %s", obj)));
  }

  private static void printStack(final Object[] stack) {}

  //    public static MethodSignature fromSignature(final int access, final String name, final
  // String desc,
  //                                             final String signature, final String[] exceptions)
  // {
  //        return new MethodSignature(access, name, desc, signature, exceptions);
  //    }

  //    public static class MethodSignature {
  //
  //        private final int access;
  //        private final String name;
  //        private final String desc;
  //        private final String signature;
  //        private final String[] exceptions;
  //
  //        public MethodSignature(final int access, final String name, final String desc, final
  // String signature, final String[] exceptions) {
  //            this.access = access;
  //            this.name = name;
  //            this.desc = desc;
  //            this.signature = signature;
  //            this.exceptions = exceptions;
  //        }
  //
  //        public int getAccess() {
  //            return access;
  //        }
  //
  //        public String getName() {
  //            return name;
  //        }
  //
  //        public String getDesc() {
  //            return desc;
  //        }
  //
  //        public String getSignature() {
  //            return signature;
  //        }
  //
  //        public String[] getExceptions() {
  //            return exceptions;
  //        }
  //    }

}
