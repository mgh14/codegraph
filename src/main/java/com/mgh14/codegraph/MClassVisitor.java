package com.mgh14.codegraph;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

/** */
@EqualsAndHashCode(callSuper = true)
@Value
public class MClassVisitor extends ClassVisitor {

  private final Map<String, List<MMethodVisitor.MethodVisit>> classMethodCalls = new HashMap<>();
  private final String parentClass;

  public MClassVisitor(int api, String parentClass) {
    super(api, null);
    this.parentClass = parentClass;
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String desc, String signature, String[] exceptions) {
    String constructorText = isConstructorMethod(name) ? "CONSTRUCTOR" : StringUtils.EMPTY;
    String staticOrInstance = isClInitBlock(name) ? "inst" : "stat";
    String methodId =
        String.format(
            "parent:[%s]::access:[%s];name:[%s];desc:[%s][%s];signature:[%s];exceptions:[%s];s-o-i:[%s]",
            parentClass,
            access,
            name,
            constructorText,
            desc,
            signature,
            Arrays.toString(exceptions),
            staticOrInstance);
    // Note: I hate that I have to pass in 'classMethodCalls' here and have it mutated by the
    // MMethodVisitor object, but I'm not able to figure out any other way to gather and retrieve
    // the data collected inside this class because this seems to be the only handle to the
    // MMethodVisitor
    //noinspection UnnecessaryLocalVariable
    MMethodVisitor singleMethodVisitor = new MMethodVisitor(methodId, classMethodCalls);
    return singleMethodVisitor;
  }

  public Map<String, List<MMethodVisitor.MethodVisit>> getReferenced() {
    return Collections.unmodifiableMap(classMethodCalls);
  }

  private static boolean isConstructorMethod(String name) {
    return "<init>".equals(name);
  }

  private static boolean isClInitBlock(String name) {
    return "<clinit>".equals(name);
  }
}
