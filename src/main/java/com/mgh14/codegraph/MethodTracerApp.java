package com.mgh14.codegraph;

import com.mgh14.codegraph.adapter.javap.dto.MethodCall;
import com.mgh14.codegraph.tracer.MethodVisitorPrinter;
import com.mgh14.codegraph.tracer.TraceClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.mgh14.codegraph.util.ClassUtils.*;

/** TODO: Document */
@Slf4j
public class MethodTracerApp {

  public static void main(String[] args) throws Exception {
    trace(getClassAsStream(MethodTracerApp.class, ForLookingAtBytesClass.class));
  }

  public static void trace(InputStream in) throws IOException {
    final ClassReader classReader = new ClassReader(in);
    printClassReaderInformation(classReader);

    log.info("Beginning trace...");
    final TraceClassVisitor traceVisitor = new TraceClassVisitor(new MethodVisitorPrinter());
    classReader.accept(traceVisitor, ClassReader.EXPAND_FRAMES);

    final Map<String, List<MethodCall>> invoked = traceVisitor.getCalled();
    log.info("Methods Size: " + invoked.size());
    invoked.forEach(
        (key, value1) -> {
          log.info("Methods from class " + key);
          value1.forEach(
              value -> {
                log.info("\tOpcode: " + identifyOpcode(value.getOpcode()));
                log.info("\tName: " + value.getName());
                log.info("\tDesc: " + value.getDesc());
                log.info("\tItf: " + value.isItf());
                log.info("");
              });
        });
  }
}
