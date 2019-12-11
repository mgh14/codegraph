package com.mgh14.codegraph;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static com.mgh14.codegraph.util.ClassUtils.getClassAsStream;
import static com.mgh14.codegraph.util.ClassUtils.printClassReaderInformation;
import static org.objectweb.asm.Opcodes.ASM5;

/** TODO: Document */
@Slf4j
public class CodeGraphApp {

  public static void main(String[] args) throws Exception {
    InputStream in = getClassAsStream(CodeGraphApp.class, ForLookingAtBytesClass.class);
    analyzeMethodRefs(in);
  }

  public static void analyzeMethodRefs(InputStream in) throws IOException {
    ClassReader classReader = new ClassReader(in);
    printClassReaderInformation(classReader);
    String className = classReader.getClassName();

    log.info(String.format("Beginning trace for class [%s]...", className));
    MClassVisitor mClassVisitor = new MClassVisitor(ASM5, className);
    TraceClassVisitor traceVisitor =
        new TraceClassVisitor(mClassVisitor, new PrintWriter(System.out));
    classReader.accept(traceVisitor, ClassReader.EXPAND_FRAMES);
      Map<String, List<MMethodVisitor.MethodVisit>> parentClassMethodVisitsByMethodId = mClassVisitor.getReferenced();
    int x = 5;  // TODO: temporary stopping point for debugging; needs removed
  }
}
