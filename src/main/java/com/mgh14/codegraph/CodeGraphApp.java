package com.mgh14.codegraph;

import com.mgh14.codegraph.filter.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.mgh14.codegraph.util.ClassUtils.getClassAsStream;
import static com.mgh14.codegraph.util.ClassUtils.getExternalName;
import static org.objectweb.asm.Opcodes.ASM7;

/** TODO: Document */
@Slf4j
public class CodeGraphApp {

  public static final int ASM_VERSION = ASM7;
  private static Map<Class<?>, Exception> errors = new HashMap<>();

  public static void main(String[] args) throws Exception {
    Map<MethodReference, List<MethodInstructionReference>> analysisResult =
        analyzeMethodRefs(ForLookingAtBytesClass.class);
    int x = 5; // TODO: temporary stopping point for debugging; needs removed
  }

  public static Map<MethodReference, List<MethodInstructionReference>> analyzeMethodRefs(
      Class<?> clazz) throws ClassNotFoundException {
    return analyzeMethodRefs(
        clazz, new HashSet<>(), CompositeFilter.ofFilters(new NonObjectFilter()));
  }

  private static Map<MethodReference, List<MethodInstructionReference>> analyzeMethodRefs(
      Class<?> clazz, Set<Class<?>> visitedClasses, Filter classFilter)
      throws ClassNotFoundException {
    log.info(
        "Analyzing class [{}] (classes analyzed so far: [{}])",
        clazz.getName(),
        visitedClasses.size());
    if (visitedClasses.contains(clazz)) {
      log.info("Class [{}] analysis already done. Returning empty...", clazz.getName());
      return Collections.emptyMap();
    }
    InputStream in = getClassAsStream(CodeGraphApp.class, clazz);
    ClassReader classReader;
    try {
      classReader = new ClassReader(in);
    } catch (IOException e) {
      log.error("Class not found: {}", clazz.getName(), e);
      errors.put(clazz, e);
      visitedClasses.add(clazz);
      return Collections.emptyMap();
    }

    // TODO: since this program can take awhile to run, should we not enable this?
    // printClassReaderInformation(classReader);
    String className = classReader.getClassName();

    // visit parent ref (i.e. given Class<?> object)
    log.info("Beginning trace for class [{}]...", className);
    MClassVisitor mClassVisitor = new MClassVisitor(ASM_VERSION, className);
    TraceClassVisitor traceVisitor = new TraceClassVisitor(mClassVisitor, null);
    classReader.accept(traceVisitor, ClassReader.EXPAND_FRAMES);
    Map<MethodReference, List<MethodInstructionReference>> parentClassMethodVisitsByMethodId =
        mClassVisitor.getReferenced();
    visitedClasses.add(clazz);
    log.info("Finished trace for class [{}]", className);

    // compute and visit child refs:
    Set<MethodInstructionReference> childRefsToVisit =
        calculateChildRefsToVisit(parentClassMethodVisitsByMethodId);
    Map<MethodReference, List<MethodInstructionReference>> allClassMethodVisitsByMethodId =
        new HashMap<>(parentClassMethodVisitsByMethodId);
    for (MethodInstructionReference methodInstructionReference : childRefsToVisit) {
      String classExternalName = getExternalName(methodInstructionReference.getOwner());
      log.debug(
          "Checking if class [{}] has been visited and/or is not filtered out...",
          classExternalName);
      if (!classIsVisited(classExternalName, visitedClasses)
          && (Objects.isNull(classFilter) || !classFilter.filterOutClass(classExternalName))) {
        log.debug("Class [{}] will be visited.", classExternalName);
        Map<MethodReference, List<MethodInstructionReference>> childVisitResults =
            analyzeMethodRefs(ClassUtils.getClass(classExternalName), visitedClasses, classFilter);
        for (Map.Entry<MethodReference, List<MethodInstructionReference>> childVisitResult :
            childVisitResults.entrySet()) {
          allClassMethodVisitsByMethodId.putIfAbsent(
              childVisitResult.getKey(), childVisitResult.getValue());
        }
      }
    }
    return allClassMethodVisitsByMethodId;
  }

  private static boolean classIsVisited(String className, Set<Class<?>> visitedClasses) {
    return visitedClasses.stream()
        .map(Class::getCanonicalName)
        .anyMatch(s -> Objects.equals(className, s));
  }

  private static Set<MethodInstructionReference> calculateChildRefsToVisit(
      Map<MethodReference, List<MethodInstructionReference>> parentClassMethodVisitsByMethodId) {
    return parentClassMethodVisitsByMethodId.entrySet().stream()
        .flatMap(entry -> entry.getValue().stream())
        .collect(Collectors.toSet());
  }
}
