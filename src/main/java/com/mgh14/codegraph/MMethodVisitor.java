package com.mgh14.codegraph;

import com.mgh14.codegraph.util.ClassUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM5;

/** */
public class MMethodVisitor extends MethodVisitor {

  @Value
  @Builder
  public static class MethodVisit {
    String owner;
    MethodReference thisRef;
    MethodReference parentRef;
    List<MethodVisit> childMethodVisits;
  }

  private static final PrintStream printStream = System.out;
  private static final String END_DELIMITER = " //]";

  private final String idOfMethodToVisit;
  // Note: while this variable is private and final, it is NOT meant to be immutable; only the
  // reference is meant to be constant. There seems to be no other way of collecting the information
  // garnered in this class while parsing a method other than to give this class a data structure
  // with an exterior handle that can store the information parsed here
  private final Map<String, List<MMethodVisitor.MethodVisit>> parentClassMethodVisitsByMethodId;

  // TODO: pass in ClassReference (or something) pointing to parent of this method?
  public MMethodVisitor(
      String idOfMethodToVisit, Map<String, List<MethodVisit>> parentClassMethodVisitsByMethodId) {
    super(ASM5);
    this.idOfMethodToVisit = idOfMethodToVisit;
    this.parentClassMethodVisitsByMethodId = parentClassMethodVisitsByMethodId;
    this.parentClassMethodVisitsByMethodId.computeIfAbsent(
        idOfMethodToVisit, ignored -> new ArrayList<>());
  }

  @Override
  public void visitCode() {
    print("VISIT_CODE: Beginning visit to method...", true);
    super.visitCode();
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    MethodReference currentMethodRef = new MethodReference(opcode, name, desc, itf);
    MethodVisit currentVisit = MethodVisit.builder().owner(owner).thisRef(currentMethodRef).build();
    parentClassMethodVisitsByMethodId.get(idOfMethodToVisit).add(currentVisit);

    printInsnVisit("VISIT_METHOD_INSN", opcode, owner, name, desc);
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
    super.visitEnd();
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

  private void print(String s) {
    print(s, false);
  }

  private void print(String s, boolean requiresIndent) {
    String initialIndent = requiresIndent ? "\t" : StringUtils.EMPTY;
    printStream.println(String.format("%s%s%s", initialIndent, s, END_DELIMITER));
  }
}
