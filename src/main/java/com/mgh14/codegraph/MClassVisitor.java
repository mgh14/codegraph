package com.mgh14.codegraph;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** */
public class MClassVisitor extends ClassVisitor {

  private final Map<MethodReference, List<MethodInstructionReference>> classMethodCalls =
      new HashMap<>();
  private final String parentClass;

  public MClassVisitor(int api, String parentClass) {
    super(api, null);
    this.parentClass = parentClass;
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String desc, String signature, String[] exceptions) {
    MethodReference methodRef =
        MethodReference.builder()
            .parentClass(parentClass)
            .access(access)
            .isConstructor(isConstructorMethod(name))
            .isStatic(isClInitBlock(name))
            .name(name)
            .desc(desc)
            .signature(signature)
            .exceptions(exceptions)
            .build();
    // Note: I hate that I have to pass in 'classMethodCalls' here and have it mutated by the
    // MMethodVisitor object, but I'm not able to figure out any other way to gather and retrieve
    // the data collected inside this class because this seems to be the only handle to the
    // MMethodVisitor
    //noinspection UnnecessaryLocalVariable
    MMethodVisitor singleMethodVisitor = new MMethodVisitor(methodRef, classMethodCalls);
    return singleMethodVisitor;
  }

  public Map<MethodReference, List<MethodInstructionReference>> getReferenced() {
    return Collections.unmodifiableMap(classMethodCalls);
  }

  private static boolean isConstructorMethod(String name) {
    return "<init>".equals(name);
  }

  private static boolean isClInitBlock(String name) {
    return "<clinit>".equals(name);
  }
}
