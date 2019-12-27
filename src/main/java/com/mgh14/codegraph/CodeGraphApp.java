package com.mgh14.codegraph;

import com.mgh14.codegraph.filter.*;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.mgh14.codegraph.util.ClassUtils.*;
import static com.mgh14.codegraph.util.ClassUtils.getClassAsStream;
import static com.mgh14.codegraph.util.ClassUtils.getExternalName;
import static org.objectweb.asm.Opcodes.ASM7;

/** TODO: Document */
@Slf4j
public class CodeGraphApp {

  public static final int ASM_VERSION = ASM7;
  private static Map<Class<?>, Exception> errors = new HashMap<>();

  public static void main(String[] args) throws Exception {
    Class<?> classToAnalyze = ForLookingAtBytesClass.class;
    Map<MethodReference, CallTreeNode> allCallGraphs = getAllCallGraphs(classToAnalyze);
    int x = 5; // TODO: temporary stopping point for debugging; needs removed
  }

  @Value
  // TODO: not private right now so it can be testable
  public static class CallTreeNode {
    String owner;
    MethodReference referenceToMethodThatCallsThisMethod;
    MethodInstructionReference referringMethodInstruction;
    List<CallTreeNode> children;
  }

  // TODO: this method is not complete. Right now it only gathers the children for the root method
  // references.
  // TODO: not private right now so it can be testable
  static Map<MethodReference, CallTreeNode> getAllCallGraphs(Class<?> clazz)
      throws ClassNotFoundException {
    Map<MethodReference, List<MethodInstructionReference>> analysisResult =
        analyzeMethodRefs(clazz).getAllMethodRefs();

    //      Map<String, List<MethodReference>> methodRefsByClass2 =
    // analysisResult.keySet().stream().collect(Collectors.groupingBy((entry ->
    // entry.getParentClass())));

    Set<String> parentClasses =
        analysisResult.keySet().stream().map(mr -> mr.getParentClass()).collect(Collectors.toSet());
    Map<String, Map<MethodReference, List<MethodInstructionReference>>> methodRefsByClass =
        new HashMap<>();
    parentClasses.forEach(
        parentClass ->
            methodRefsByClass.put(
                parentClass,
                analysisResult.entrySet().stream()
                    .filter(entry -> entry.getKey().getParentClass().equals(parentClass))
                    .collect(
                        Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()))));

    String internalClassName = getInternalNameWithoutClass(clazz);
    Map<MethodReference, CallTreeNode> startingFromAnyMethodCallTree = new HashMap<>();
    Map<MethodReference, List<MethodInstructionReference>> chosenClassMethods =
        methodRefsByClass.get(internalClassName);
    // For each root-level method in the analyzed class:
    for (MethodReference analyzedClassMethodRef : chosenClassMethods.keySet()) {
      MethodReference parentMethodReference = analyzedClassMethodRef;
      List<MethodInstructionReference> mIrs = chosenClassMethods.get(parentMethodReference);
      if (!startingFromAnyMethodCallTree.containsKey(parentMethodReference)) {
        // Since this is a root node (i.e. a method in the targeted/analyzed class, there is no
        // parent method and thus no method instruction. This is one of the class "entry points"
        CallTreeNode analyzedMethodEntryPoint =
            new CallTreeNode(internalClassName, null, null, new ArrayList<>());
        startingFromAnyMethodCallTree.put(parentMethodReference, analyzedMethodEntryPoint);
      }
      // For each method instruction inside of the root-level method (of the analyzed class):
      for (MethodInstructionReference instructionRef : mIrs) {
        // Find the method reference referred to by the current instruction reference from the list
        // of all method refs by class above:
        // TODO: refactor finding the MethodReference (based on a MethodInstructionReference) into a
        // separate method
        Map<MethodReference, List<MethodInstructionReference>> methodRefsOfOneClass =
            methodRefsByClass.get(instructionRef.getOwner());
        if (methodRefsOfOneClass == null) {
          // TODO: best way to represent this case?
          continue;
        }
        MethodReference childMethodRef =
            methodRefsByClass.get(instructionRef.getOwner()).keySet().stream()
                .filter(key -> instructionMatchesMethodSignature(instructionRef, key))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No match found!"));
        if (!startingFromAnyMethodCallTree.containsKey(childMethodRef)) {
          startingFromAnyMethodCallTree.put(
              childMethodRef,
              new CallTreeNode(
                  internalClassName, parentMethodReference, instructionRef, new ArrayList<>()));
        }

        CallTreeNode callTreeNode = startingFromAnyMethodCallTree.get(parentMethodReference);
        CallTreeNode callTreeChildNode = startingFromAnyMethodCallTree.get(childMethodRef);
        List<CallTreeNode> nodeChildren = callTreeNode.getChildren();
        if (!nodeChildren.contains(callTreeChildNode)) {
          nodeChildren.add(callTreeChildNode);
        }
      }
    }

    return startingFromAnyMethodCallTree;
  }

  @Value
  public static class MethodsAnalysis {
    Set<MethodReference> rootMethodRefs;    // i.e. the methods from the analyzed class, the roots of the call tree
    Map<MethodReference, List<MethodInstructionReference>> allMethodRefs;
  }

  public static MethodsAnalysis analyzeMethodRefs(Class<?> clazz) throws ClassNotFoundException {
    return recursiveAnalyzeMethodRefs(
        clazz, new HashSet<>(), CompositeFilter.ofFilters(new NonObjectFilter()));
  }

  private static MethodsAnalysis recursiveAnalyzeMethodRefs(
      Class<?> clazz, Set<Class<?>> visitedClasses, Filter classFilter)
      throws ClassNotFoundException {
    log.info(
        "Analyzing class [{}] (classes analyzed so far: [{}])",
        clazz.getName(),
        visitedClasses.size());

    // checking pre-conditions:
    if (visitedClasses.contains(clazz)) {
      log.info("Class [{}] analysis already done. Returning empty...", clazz.getName());
      return new MethodsAnalysis(Collections.emptySet(), Collections.emptyMap());
    }
    InputStream in = getClassAsStream(CodeGraphApp.class, clazz);
    ClassReader classReader;
    try {
      classReader = new ClassReader(in);
    } catch (IOException e) {
      log.error("Class not found: {}", clazz.getName(), e);
      errors.put(clazz, e);
      visitedClasses.add(clazz);
      return new MethodsAnalysis(Collections.emptySet(), Collections.emptyMap());
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
    Map<MethodReference, List<MethodInstructionReference>> allClassMethodVisitsByMethodReference =
        new HashMap<>(parentClassMethodVisitsByMethodId);
    for (MethodInstructionReference methodInstructionReference : childRefsToVisit) {
      String classExternalName = getExternalName(methodInstructionReference.getOwner());
      log.debug(
          "Checking if class [{}] has been visited and/or is not filtered out...",
          classExternalName);
      if (!classIsVisited(classExternalName, visitedClasses)
          && (Objects.isNull(classFilter) || !classFilter.filterOutClass(classExternalName))) {
        log.debug("Class [{}] will be visited.", classExternalName);
        MethodsAnalysis childVisitResults =
            recursiveAnalyzeMethodRefs(
                ClassUtils.getClass(classExternalName), visitedClasses, classFilter);
        for (Map.Entry<MethodReference, List<MethodInstructionReference>> childVisitResult :
            childVisitResults.getAllMethodRefs().entrySet()) {
          allClassMethodVisitsByMethodReference.putIfAbsent(
              childVisitResult.getKey(), childVisitResult.getValue());
        }
      }
    }
    return new MethodsAnalysis(null, allClassMethodVisitsByMethodReference);
  }

  private static boolean instructionMatchesMethodSignature(
      MethodInstructionReference mir, MethodReference ms) {
    return mir.getDesc().equals(ms.getDesc()) && mir.getName().equals(ms.getName());
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
