package com.mgh14.codegraph.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Printer;

import java.io.InputStream;
import java.util.Arrays;

@UtilityClass
@Slf4j
public class ClassUtils {

  public static InputStream getClassAsStream(Class<?> loadingClass, Class<?> classToLoad) {
    return loadingClass.getResourceAsStream(getInternalName(classToLoad));
  }

  public static String getExternalName(String internalName) {
      return internalName.replace("/", ".");
  }

  public static String getInternalName(Class<?> clazz) {
    return "/" + getInternalNameWithoutClass(clazz) + ".class";
  }

  public static String getInternalNameWithoutClass(Class<?> clazz) {
      return clazz.getName().replace('.', '/');
  }

  public static String identifyOpcode(int opcode) {
    return Printer.OPCODES[opcode] + " (op int " + opcode + ")";
  }

  public static void printClassReaderInformation(ClassReader classReader) {

    log.info("\nClassReader Information:\n--------------------");
    log.info("Access: " + classReader.getAccess());
    log.info("Class Name: " + classReader.getClassName());
    log.info("Super Name: " + classReader.getSuperName());
    log.info("Interfaces: " + Arrays.asList(classReader.getInterfaces()));
    log.info("Item count: " + classReader.getItemCount());
    if (classReader.getItemCount() > 0) {
      for (int i = 0; i < classReader.getItemCount(); i++) {
        log.info("\tItem Index: " + classReader.getItem(i));
      }
    }
    log.info("Max String Length: " + classReader.getMaxStringLength());
  }
}
