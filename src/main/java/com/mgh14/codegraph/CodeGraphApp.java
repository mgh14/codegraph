package com.mgh14.codegraph;

import com.mgh14.codegraph.filter.CompositeFilter;
import com.mgh14.codegraph.filter.Filter;
import com.mgh14.codegraph.filter.NonObjectFilter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mgh14.codegraph.util.ClassUtils.*;
import static org.objectweb.asm.Opcodes.ASM7;

/** TODO: Document */
@Slf4j
public class CodeGraphApp {

  public static final int ASM_VERSION = ASM7;
  private static Map<Class<?>, Exception> errors = new HashMap<>();

  public static void main(String[] args) throws Exception {
    Class<?> classToAnalyze = loadClass(args[1], args[2]);
    Map<String, CallTreeNodeDetail> allCallGraphsOneChildDeep =
        getAllCallGraphsOneChildDeep(classToAnalyze);
    // now we need to piece together all the call graphs that are only one child deep right now:
    Set<String> unprocessedNodeKeys = new HashSet<>(allCallGraphsOneChildDeep.keySet());
    Collection<CallTreeNodeDetail> allRefs = allCallGraphsOneChildDeep.values();
    Set<String> leafCallTreeNodeKeys =
        allCallGraphsOneChildDeep.entrySet().stream()
            .filter(entry -> entry.getValue().getChildren().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    for (String callTreeNodeKey : leafCallTreeNodeKeys) {
      CallTreeNodeDetail currentLeafNode = allCallGraphsOneChildDeep.get(callTreeNodeKey);
      MethodReference mr = currentLeafNode.getReferenceToMethodThatCallsThisMethod();
      if (Objects.isNull(mr)) {
        // this is both a leaf node and the root of a graph that calls no other methods in the
        // root/analyzed class; processing for this method is done
        unprocessedNodeKeys.remove(currentLeafNode.getThisMethodReference().toString());
        continue;
      }
      Set<CallTreeNodeDetail> nodesWithMethodRefMatchingLeafReferringMethodInstruction =
          allRefs.stream()
              .filter(ref -> ref.getThisMethodReference().equals(mr))
              .collect(Collectors.toSet());
      if (nodesWithMethodRefMatchingLeafReferringMethodInstruction.size() != 1) {
        throw new RuntimeException(
            "Illegal state! No single call tree node found for method ref {" + mr + "}");
      }
      CallTreeNodeDetail nodeCallingLeafViaMethodInstructionReference =
          nodesWithMethodRefMatchingLeafReferringMethodInstruction.iterator().next();
      if (nodeCallingLeafViaMethodInstructionReference
          .getThisMethodReference()
          .equals(currentLeafNode.getThisMethodReference())) {
        CallTreeNodeDetail circularNodeReference =
            new CallTreeNodeDetail(
                currentLeafNode.getOwner(),
                currentLeafNode.getReferenceToMethodThatCallsThisMethod(),
                currentLeafNode.getReferringMethodInstruction(),
                new ArrayList<>(0),
                currentLeafNode.getThisMethodReference());
        nodeCallingLeafViaMethodInstructionReference.getChildren().add(circularNodeReference);
      } else if (!nodeCallingLeafViaMethodInstructionReference
          .getChildren()
          .contains(currentLeafNode)) {
        nodeCallingLeafViaMethodInstructionReference.getChildren().add(currentLeafNode);
      } else {
        log.debug(""); // TODO: what to put here?
      }
    }

    String analyzedClassName = getInternalNameWithoutClass(classToAnalyze);
    Map<String, CallTreeNodeDetail> analyzedClassGraphs =
        allCallGraphsOneChildDeep.entrySet().stream()
            .filter(
                ref ->
                    ref.getValue()
                        .getThisMethodReference()
                        .getParentClass()
                        .equals(analyzedClassName))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    log.info(
        String.format(
            "Finished analysis of class [%s]. Outputting method graphs...", analyzedClassName));
    for (CallTreeNodeDetail methodGraph : analyzedClassGraphs.values()) {
      outputGraph(methodGraph, 0);
    }
    int x = 5; // TODO: temporary stopping point for debugging; needs removed
  }

    private static Class<?> loadClass(String pathToJar, String fullClassName)
            throws MalformedURLException, ClassNotFoundException {
        File file = new File(pathToJar);

        URL url = file.toURI().toURL();
        URL[] urls = new URL[] {url};

        ClassLoader cl = new URLClassLoader(urls);
        return cl.loadClass(fullClassName);
    }

  private static void outputGraph(CallTreeNodeDetail node, int level) {
    String methodRefName =
        node.getOwner()
            + "#"
            + node.getThisMethodReference().getName()
            + String.format(
                " (method hash: %s, children = %d)",
                node.getThisMethodReference().hashCode(), node.getChildren().size());
    if (level == 0) {
      log.info("Root Method: " + methodRefName);
    } else {
      String indent =
          IntStream.rangeClosed(0, level)
              .mapToObj(i -> "\t")
              .collect(Collectors.joining(StringUtils.EMPTY));
      log.info(indent + "NR: " + methodRefName);
    }
    for (CallTreeNodeDetail child : node.getChildren()) {
      outputGraph(child, level + 1);
    }
  }

  @Value
  public static class CallTreeNodeDetail {
    String owner;
    MethodReference referenceToMethodThatCallsThisMethod;
    MethodInstructionReference referringMethodInstruction;
    List<CallTreeNodeDetail> children;
    MethodReference thisMethodReference;
  }

  // TODO: this method is not complete. Right now it only gathers the children for the root method
  // references.
  // TODO: not private right now so it can be testable
  static Map<String, CallTreeNodeDetail> getAllCallGraphsOneChildDeep(Class<?> clazz)
      throws ClassNotFoundException {
    Map<MethodReference, List<MethodInstructionReference>> analysisResult =
        analyzeMethodRefs(clazz).getAllMethodRefs();

    //      Map<String, List<MethodReference>> methodRefsByClass2 =
    // analysisResult.keySet().stream().collect(Collectors.groupingBy((entry ->
    // entry.getParentClass())));

    Set<String> parentClasses =
        analysisResult.keySet().stream()
            .map(MethodReference::getParentClass)
            .collect(Collectors.toSet());
    Map<String, Map<MethodReference, List<MethodInstructionReference>>> methodRefsByClass =
        new HashMap<>();
    parentClasses.forEach(
        parentClass ->
            methodRefsByClass.put(
                parentClass,
                analysisResult.entrySet().stream()
                    .filter(entry -> entry.getKey().getParentClass().equals(parentClass))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));

    String internalClassName = getInternalNameWithoutClass(clazz);
    Map<MethodReference, CallTreeNodeDetail> startingFromAnyMethodCallTree = new HashMap<>();
    Map<MethodReference, List<MethodInstructionReference>> chosenClassMethods =
        methodRefsByClass.get(internalClassName);
    // For each root-level method in the analyzed class:
    for (MethodReference analyzedClassMethodRef : chosenClassMethods.keySet()) {
      @SuppressWarnings("UnnecessaryLocalVariable")
      MethodReference parentMethodReference = analyzedClassMethodRef;
      List<MethodInstructionReference> mIrs = chosenClassMethods.get(parentMethodReference);
      if (!startingFromAnyMethodCallTree.containsKey(parentMethodReference)) {
        // Since this is a root node (i.e. a method in the targeted/analyzed class), there is no
        // parent method and thus no method instruction. This is one of the class "entry points"
        CallTreeNodeDetail analyzedMethodEntryPoint =
            new CallTreeNodeDetail(
                internalClassName, null, null, new ArrayList<>(), parentMethodReference);
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
        if (!childMethodRef.equals(parentMethodReference)
            && !startingFromAnyMethodCallTree.containsKey(childMethodRef)) {
          startingFromAnyMethodCallTree.put(
              childMethodRef,
              new CallTreeNodeDetail(
                  internalClassName,
                  parentMethodReference,
                  instructionRef,
                  new ArrayList<>(),
                  childMethodRef));
        }

        CallTreeNodeDetail callTreeNode = startingFromAnyMethodCallTree.get(parentMethodReference);
        CallTreeNodeDetail callTreeChildNode = startingFromAnyMethodCallTree.get(childMethodRef);
        List<CallTreeNodeDetail> nodeChildren = callTreeNode.getChildren();
        if (!childMethodRef.equals(parentMethodReference)
            && !nodeChildren.contains(callTreeChildNode)) {
          nodeChildren.add(callTreeChildNode);
        }
      }
    }

    Map<String, CallTreeNodeDetail> transformedStartingFromAnyMethodCallTree =
        startingFromAnyMethodCallTree.keySet().stream()
            .collect(
                Collectors.toMap(MethodReference::toString, startingFromAnyMethodCallTree::get));
    return transformedStartingFromAnyMethodCallTree;
  }

  @Value
  public static class MethodsAnalysis {
    Set<MethodReference>
        rootMethodRefs; // i.e. the methods from the analyzed class, the roots of the call tree
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
      } else {
        log.debug("Class [{}] has already been visited.", classExternalName);
      }
    }
    return new MethodsAnalysis(null, allClassMethodVisitsByMethodReference);
  }

  private static boolean instructionMatchesMethodSignature(
      MethodInstructionReference methodInsRef, MethodReference methodRef) {
    return Objects.nonNull(methodInsRef)
        && methodInsRef.getOwner().equals(methodRef.getParentClass())
        && methodInsRef.getDesc().equals(methodRef.getDesc())
        && methodInsRef.getName().equals(methodRef.getName());
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
