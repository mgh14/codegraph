package com.mgh14.codegraph;

import com.mgh14.codegraph.util.ClassUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.mgh14.codegraph.CodeGraphApp.ASM_VERSION;

/** */
@Slf4j
public class MMethodVisitor extends MethodVisitor {

  @Value
  @Builder
  public static class MethodVisit {
    String methodId;
    String owner;
    MethodReference thisRef;
    MethodReference parentRef;
    List<MethodVisit> childMethodVisits;
  }

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
    super(ASM_VERSION);
    this.idOfMethodToVisit = idOfMethodToVisit;
    this.parentClassMethodVisitsByMethodId = parentClassMethodVisitsByMethodId;
    this.parentClassMethodVisitsByMethodId.computeIfAbsent(
        idOfMethodToVisit, ignored -> new ArrayList<>());
  }

  @Override
  public void visitCode() {
    log(() -> "VISIT_CODE: Beginning visit to method...", true);
    super.visitCode();
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    String noLName = name.replace("[L", StringUtils.EMPTY);
    MethodReference currentMethodRef = new MethodReference(opcode, noLName, desc, itf);
    MethodVisit currentVisit =
        MethodVisit.builder()
            .methodId(idOfMethodToVisit)
            .owner(owner)
            .thisRef(currentMethodRef)
            .childMethodVisits(new ArrayList<>())
            .build();
    parentClassMethodVisitsByMethodId.get(idOfMethodToVisit).add(currentVisit);

    printInsnVisit("VISIT_METHOD_INSN", opcode, owner, name, desc);
  }

  @Override
  public void visitInsn(int opcode) {
    log(() -> String.format("VISIT_INSN: opcode: %s", ClassUtils.identifyOpcode(opcode)));
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    printInsnVisit("VISIT_FIELD_INSN", opcode, owner, name, desc);
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    log(
        () ->
            String.format(
                "VISIT_VAR_INSN: opcode: %s; var: %s", ClassUtils.identifyOpcode(opcode), var));
  }

  @Override
  public void visitLocalVariable(
      String name, String desc, String signature, Label start, Label end, int index) {
    log(
        () ->
            String.format(
                "Local variable: name: %s; desc: %s; signature: %s; start: %s; end: %s; index: %d",
                name, desc, signature, start, end, index));
  }

  @Override
  public void visitEnd() {
    log(() -> "END_VISIT: visit of method ended");
    super.visitEnd();
  }

  private void printInsnVisit(
      final String prefix,
      final int opcode,
      final String owner,
      final String name,
      final String desc) {
    log(
        () ->
            String.format(
                "%s: name: %s; Opcode: %s; owner: %s; desc: %s ",
                prefix, name, ClassUtils.identifyOpcode(opcode), owner, desc));
  }

  private void log(Supplier<String> supplier) {
    log(supplier, false);
  }

  private void log(Supplier<String> supplier, boolean requiresIndent) {
    String initialIndent = requiresIndent ? "\t" : StringUtils.EMPTY;
    log.debug(String.format("%s%s%s", initialIndent, supplier.get(), END_DELIMITER));
  }
}
