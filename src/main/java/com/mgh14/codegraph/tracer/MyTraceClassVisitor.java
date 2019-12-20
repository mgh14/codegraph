package com.mgh14.codegraph.tracer;

import com.mgh14.codegraph.MethodReference;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM7;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
public class MyTraceClassVisitor extends ClassVisitor {

    private final MethodVisitorPrinter methodVisitorPrinter;

    public MyTraceClassVisitor(final MethodVisitorPrinter methodVisitorPrinter) {
        this(ASM7, methodVisitorPrinter);
    }

    public MyTraceClassVisitor(final int api, final MethodVisitorPrinter methodVisitorPrinter) {
        super(api, null);
        this.methodVisitorPrinter = methodVisitorPrinter;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return methodVisitorPrinter;
    }

    public Map<String, List<MethodReference>> getCalled() {
        return methodVisitorPrinter.getVisitedMethods();
    }

    /*public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.p.visit(version, access, name, signature, superName, interfaces);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public void visitSource(String file, String debug) {
        this.p.visitSource(file, debug);
        super.visitSource(file, debug);
    }

    public void visitOuterClass(String owner, String name, String desc) {
        this.p.visitOuterClass(owner, name, desc);
        super.visitOuterClass(owner, name, desc);
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        Printer p = this.p.visitClassAnnotation(desc, visible);
        AnnotationVisitor av = this.cv == null?null:this.cv.visitAnnotation(desc, visible);
        return new TraceAnnotationVisitor(av, p);
    }

    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        Printer p = this.p.visitClassTypeAnnotation(typeRef, typePath, desc, visible);
        AnnotationVisitor av = this.cv == null?null:this.cv.visitTypeAnnotation(typeRef, typePath, desc, visible);
        return new TraceAnnotationVisitor(av, p);
    }

    public void visitAttribute(Attribute attr) {
        // what is this?
        this.p.visitClassAttribute(attr);
        super.visitAttribute(attr);
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        this.p.visitInnerClass(name, outerName, innerName, access);
        super.visitInnerClass(name, outerName, innerName, access);
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        Printer p = this.p.visitField(access, name, desc, signature, value);
        FieldVisitor fv = this.cv == null?null:this.cv.visitField(access, name, desc, signature, value);
        return new TraceFieldVisitor(fv, p);
    }

    public void visitEnd() {
        this.p.visitClassEnd();
        if(this.pw != null) {
            this.p.print(this.pw);
            this.pw.flush();
        }

        super.visitEnd();
    }*/
}
