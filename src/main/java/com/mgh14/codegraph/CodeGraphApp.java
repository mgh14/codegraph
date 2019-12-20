package com.mgh14.codegraph;

import com.mgh14.codegraph.filter.CompositeFilter;
import com.mgh14.codegraph.filter.Filter;
import com.mgh14.codegraph.filter.JavaUtilFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
    Map<String, List<MMethodVisitor.MethodVisit>> analysisResult =
        analyzeMethodRefs(ForLookingAtBytesClass.class);
    int x = 5; // TODO: temporary stopping point for debugging; needs removed
  }

  public static Map<String, List<MMethodVisitor.MethodVisit>> analyzeMethodRefs(Class<?> clazz)
      throws IOException, ClassNotFoundException {
    return analyzeMethodRefs(
        clazz, new HashSet<>(), CompositeFilter.ofFilters(new JavaUtilFilter()));
  }

  private static Map<String, List<MMethodVisitor.MethodVisit>> analyzeMethodRefs(
      Class<?> clazz, Set<Class<?>> visitedClasses, Filter classFilter)
      throws IOException, ClassNotFoundException {
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

    // TODO: since this program takes awhile to run, should we not enable this?
    // printClassReaderInformation(classReader);
    String className = classReader.getClassName();

    log.info("Beginning trace for class [{}]...", className);
    MClassVisitor mClassVisitor = new MClassVisitor(ASM_VERSION, className);
    TraceClassVisitor traceVisitor =
        new TraceClassVisitor(mClassVisitor, new PrintWriter("abcd.out"));
    classReader.accept(traceVisitor, ClassReader.EXPAND_FRAMES);

    // visit parent ref (i.e. given Class<?> object)
    Map<String, List<MMethodVisitor.MethodVisit>> parentClassMethodVisitsByMethodId =
        mClassVisitor.getReferenced();
    visitedClasses.add(clazz);
    log.info("Finished trace for class [{}]", className);

    // compute and visit child refs:
    Set<MMethodVisitor.MethodVisit> childRefsToVisit =
        calculateChildRefsToVisit(parentClassMethodVisitsByMethodId);
    Map<String, List<MMethodVisitor.MethodVisit>> allClassMethodVisitsByMethodId =
        new HashMap<>(parentClassMethodVisitsByMethodId);
    for (MMethodVisitor.MethodVisit methodVisit : childRefsToVisit) {
      String classExternalName = getExternalName(methodVisit.getOwner());
      log.debug(
          "Checking if class [{}] has been visited and/or is not filtered out...",
          classExternalName);
      if (!classIsVisited(classExternalName, visitedClasses)
          && !classFilter.filterOutClass(classExternalName)) {
        log.debug("Class [{}] will be visited.", classExternalName);
        Map<String, List<MMethodVisitor.MethodVisit>> childVisitResults =
            analyzeMethodRefs(ClassUtils.getClass(classExternalName), visitedClasses, classFilter);
        for (Map.Entry<String, List<MMethodVisitor.MethodVisit>> childVisitResult :
            childVisitResults.entrySet()) {
          allClassMethodVisitsByMethodId.putIfAbsent(
              childVisitResult.getKey(), childVisitResult.getValue());
        }
      }
    }
    int x = 5; // TODO: temporary stopping point for debugging; needs removed
    return allClassMethodVisitsByMethodId;
  }

  private static boolean classIsVisited(String className, Set<Class<?>> visitedClasses) {
    return visitedClasses.stream()
        .map(Class::getCanonicalName)
        .anyMatch(s -> Objects.equals(className, s));
  }

  private static Set<MMethodVisitor.MethodVisit> calculateChildRefsToVisit(
      Map<String, List<MMethodVisitor.MethodVisit>> parentClassMethodVisitsByMethodId) {
    return parentClassMethodVisitsByMethodId.entrySet().stream()
        .flatMap(entry -> entry.getValue().stream())
        .collect(Collectors.toSet());
  }
}
