/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.tele.method;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.field.TeleFields.InspectedMemberReifier;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Centralized collection of the {@link TeleMethodAccess}s.
 * <p>
 * The {@link INSPECTED} annotation is employed to denote methods that will be accessed remotely.
 * A field of the appropriate {@link TeleMethodAccess} subtype is generated into this file
 * by executing the {@link #main(String[])} method in this class (ensuring that the VM
 * class path contains all the {@code com.sun.max} classes).
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class TeleMethods extends AbstractTeleVMHolder {


    // Checkstyle: stop field name check

    // START GENERATED CONTENT
    public final TeleStaticMethodAccess Code_codePointerToTargetMethod = new TeleStaticMethodAccess(vm(), Code.class, "codePointerToTargetMethod", SignatureDescriptor.create("(Lcom/sun/max/unsafe/Address;)Lcom/sun/max/vm/compiler/target/TargetMethod;"));
    public final TeleStaticMethodAccess CompilationScheme$Inspect_inspectableCompilationComplete = new TeleStaticMethodAccess(vm(), CompilationScheme.Inspect.class, "inspectableCompilationComplete", SignatureDescriptor.create("(Lcom/sun/max/vm/compiler/target/TargetMethod;)V"));
    public final TeleStaticMethodAccess HeapScheme$Inspect_inspectableDecreaseMemoryRequested = new TeleStaticMethodAccess(vm(), HeapScheme.Inspect.class, "inspectableDecreaseMemoryRequested", SignatureDescriptor.create("(Lcom/sun/max/unsafe/Size;)V"));
    public final TeleStaticMethodAccess HeapScheme$Inspect_inspectableGCCompleted = new TeleStaticMethodAccess(vm(), HeapScheme.Inspect.class, "inspectableGCCompleted", SignatureDescriptor.create("()V"));
    public final TeleStaticMethodAccess HeapScheme$Inspect_inspectableGCStarted = new TeleStaticMethodAccess(vm(), HeapScheme.Inspect.class, "inspectableGCStarted", SignatureDescriptor.create("()V"));
    public final TeleStaticMethodAccess HeapScheme$Inspect_inspectableIncreaseMemoryRequested = new TeleStaticMethodAccess(vm(), HeapScheme.Inspect.class, "inspectableIncreaseMemoryRequested", SignatureDescriptor.create("(Lcom/sun/max/unsafe/Size;)V"));
    public final TeleStaticMethodAccess HeapScheme$Inspect_inspectableObjectRelocated = new TeleStaticMethodAccess(vm(), HeapScheme.Inspect.class, "inspectableObjectRelocated", SignatureDescriptor.create("(Lcom/sun/max/unsafe/Address;Lcom/sun/max/unsafe/Address;)V"));
    public final TeleStaticMethodAccess InspectableCodeInfo_inspectableCompilationComplete = new TeleStaticMethodAccess(vm(), InspectableCodeInfo.class, "inspectableCompilationComplete", SignatureDescriptor.create("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/sun/max/vm/compiler/target/TargetMethod;)V"));
    public final TeleStaticMethodAccess InspectableHeapInfo_inspectableGCCompleted = new TeleStaticMethodAccess(vm(), InspectableHeapInfo.class, "inspectableGCCompleted", SignatureDescriptor.create("(J)V"));
    public final TeleStaticMethodAccess InspectableHeapInfo_inspectableGCStarted = new TeleStaticMethodAccess(vm(), InspectableHeapInfo.class, "inspectableGCStarted", SignatureDescriptor.create("(J)V"));
    public final TeleStaticMethodAccess TargetBreakpoint_findOriginalCode = new TeleStaticMethodAccess(vm(), TargetBreakpoint.class, "findOriginalCode", SignatureDescriptor.create("(J)[B"));
    public final TeleStaticMethodAccess VmThread_detached = new TeleStaticMethodAccess(vm(), VmThread.class, "detached", SignatureDescriptor.create("()V"));
    public final TeleStaticMethodAccess VmThread_run = new TeleStaticMethodAccess(vm(), VmThread.class, "run", SignatureDescriptor.create("(Lcom/sun/max/unsafe/Pointer;Lcom/sun/max/unsafe/Pointer;Lcom/sun/max/unsafe/Pointer;)V"));
    // END GENERATED CONTENT

    // Checkstyle: resume field name check

    // CAUTION:  order-dependent declarations; these must follow the auto-generated fields.
    // Note that for this to work correctly, the named methods must be compiled into the boot heap.
    private CodeLocation compilationComplete = CodeLocation.createMachineCodeLocation(vm(), InspectableCodeInfo_inspectableCompilationComplete, "Compilation complete (internal)");
    private CodeLocation gcCompleted = CodeLocation.createMachineCodeLocation(vm(), InspectableHeapInfo_inspectableGCCompleted, "GC completed (internal)");
    private CodeLocation gcStarted = CodeLocation.createMachineCodeLocation(vm(), InspectableHeapInfo_inspectableGCStarted, "GC started (internal)");
    private CodeLocation vmThreadRunning = CodeLocation.createMachineCodeLocation(vm(), VmThread_run, "VmThread running (internal) ");
    private CodeLocation vmThreadDetached = CodeLocation.createMachineCodeLocation(vm(), VmThread_detached, "VmThread detached (internal) ");

    private final List<CodeLocation> clientInspectableMethods;

    public TeleMethods(TeleVM teleVM) {
        super(teleVM);
        // Uncomment to enable verifying that the generated content in this class is up to date when running the inspector
        // updateSource(true);
        // Note that for this to work correctly, the named methods must be compiled into the boot heap.
        final List<CodeLocation> methods = new ArrayList<CodeLocation>();
        methods.add(CodeLocation.createMachineCodeLocation(vm(), HeapScheme$Inspect_inspectableGCStarted, "Start of GC"));
        methods.add(CodeLocation.createMachineCodeLocation(vm(), HeapScheme$Inspect_inspectableGCCompleted, "End of GC"));
        methods.add(CodeLocation.createMachineCodeLocation(vm(), HeapScheme$Inspect_inspectableObjectRelocated, "Object relocated"));
        methods.add(CodeLocation.createMachineCodeLocation(vm(), CompilationScheme$Inspect_inspectableCompilationComplete, "End of method compilation"));
        clientInspectableMethods = Collections.unmodifiableList(methods);
    }

    /**
     * Identifies methods in the VM that can be offered to clients as convenient
     * stopping places.
     * <br>
     * <strong>Note</strong>: a clear separation is made between methods that are only
     * to be used by clients and those used by internal mechanisms.
     *
     * @return methods suitable for setting client-requested breakpoints.
     */
    public final List<CodeLocation> clientInspectableMethods() {
        return clientInspectableMethods;
    }

    /**
     * @return a VM method for internal (non-client) use that is called when each method compilation completes.
     */
    public CodeLocation compilationComplete() {
        return compilationComplete;
    }

    /**
     * @return a VM method for internal (non-client) use that is called whenever a thread enter its run method (equivalent to whenever a new thread is running) .
     */
    public CodeLocation vmThreadRun() {
        return vmThreadRunning;
    }

    /**
     * @return a VM method for internal (non-client) use that is called whenever a VmThread has detached itself from the active list.
     */
    public CodeLocation vmThreadDetached() {
        return vmThreadDetached;
    }

    /**
     * @return a VM method for internal (non-client) use that is called just after each GC starts.
     */
    public CodeLocation gcStarted() {
        return gcStarted;
    }

    /**
     * @return a VM method for internal (non-client) use that is called just after each GC end.
     */
    public CodeLocation gcCompleted() {
        return gcCompleted;
    }

    public static void main(String[] args) {
        Trace.begin(1, "TeleMethods updating GENERATED CONTENT");
        Trace.on(1);
        Trace.end(1, "TeleMethods updating GENERATED CONTENT");
        updateSource(false);
    }

    private static void updateSource(boolean inInspector) {
        final InspectedMemberReifier<Method> methodReifier = new InspectedMemberReifier<Method>() {
            public void reify(Method method, IndentWriter writer) {
                final Class c = method.getDeclaringClass();
                final boolean isStatic = Modifier.isStatic(method.getModifiers());
                final String holder = c.getName().substring(c.getPackage().getName().length() + 1);
                final String name = method.getName();
                final SignatureDescriptor signature = SignatureDescriptor.fromJava(method);
                final String inspectorFieldName = holder + (name.charAt(0) == '_' ? name : '_' + name);
                final String inspectorFieldType = "Tele" + (isStatic ? "Static" : "Virtual") + "MethodAccess";
                writer.println("public final " + inspectorFieldType + " " + inspectorFieldName + " = new " + inspectorFieldType + "(vm(), " +
                                holder.replace('$', '.') + ".class, \"" + name + "\", SignatureDescriptor.create(\"" + signature + "\"));");
            }
        };
        TeleFields.updateSource(TeleMethods.class, Method.class, methodReifier, inInspector);
    }
}
