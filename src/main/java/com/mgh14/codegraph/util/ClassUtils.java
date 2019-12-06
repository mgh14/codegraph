package com.mgh14.codegraph.util;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.util.Printer;

import java.io.InputStream;

@UtilityClass
public class ClassUtils {

  public static InputStream getClassAsStream(Class<?> loadingClass, Class<?> classToLoad) {
    return loadingClass.getResourceAsStream(getInternalName(classToLoad));
  }

  public static String getInternalName(Class<?> clazz) {
    return ("/" + clazz.getName().replace('.', '/') + ".class");
  }

  public static String identifyOpcode(int opcode) {
    return Printer.OPCODES[opcode] + " (op int " + opcode + ")";
  }
}
