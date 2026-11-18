/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.poc.scale;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public final class MacOsScaleAgent {
    private static final String SHELL_CLASS = "org/eclipse/swt/widgets/Shell";
    private static final String WIDGET_CLASS = "org/eclipse/swt/widgets/Widget";
    private static final String CONTROL_CLASS = "org/eclipse/swt/widgets/Control";
    private static final String PAINTER_CLASS = "org/jkiss/dbeaver/poc/scale/MacOsNativePainter";
    private static final String ACCESSIBLE_CLASS = "org/eclipse/swt/accessibility/Accessible";
    private static final String CONTROL_EVENT_CLASS = "org/eclipse/swt/accessibility/AccessibleControlEvent";
    private static final String VIEW_CLASS = "org/eclipse/swt/internal/cocoa/NSView";
    private static final String READY_PROPERTY = "dbeaver.ui.nativeScale.ready";
    private static final String SUPPORTED_SWT_VERSION = "3.135.0.v20260819-1952";

    private MacOsScaleAgent() {
    }

    public static void premain(@Nullable String arguments, @NotNull Instrumentation instrumentation) {
        if (!Boolean.getBoolean("dbeaver.ui.liveScale") || !System.getProperty("os.name").startsWith("Mac")) {
            return;
        }
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Nullable
            @Override
            public byte[] transform(
                @Nullable ClassLoader loader,
                @Nullable String className,
                @Nullable Class<?> classBeingRedefined,
                @Nullable ProtectionDomain protectionDomain,
                @NotNull byte[] classFileBuffer
            ) {
                boolean shell = SHELL_CLASS.equals(className);
                boolean accessible = ACCESSIBLE_CLASS.equals(className);
                boolean widget = WIDGET_CLASS.equals(className);
                if (!shell && !accessible && !widget) {
                    return null;
                }
                String location = protectionDomain == null || protectionDomain.getCodeSource() == null ? ""
                    : protectionDomain.getCodeSource().getLocation().toExternalForm();
                if (!location.contains(SUPPORTED_SWT_VERSION)) {
                    System.setProperty("dbeaver.ui.nativeScale.error", "Unsupported SWT version: " + location);
                    return null;
                }
                ClassReader reader = new ClassReader(classFileBuffer);
                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                int[] replacements = {0};
                reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                    private boolean hasInitializer;

                    @NotNull
                    @Override
                    public MethodVisitor visitMethod(
                        int access,
                        @NotNull String name,
                        @NotNull String descriptor,
                        @Nullable String signature,
                        @Nullable String[] exceptions
                    ) {
                        hasInitializer |= "<clinit>".equals(name);
                        MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                        boolean clientArea = shell && "getClientArea".equals(name)
                            && "()Lorg/eclipse/swt/graphics/Rectangle;".equals(descriptor);
                        boolean accessibleBounds = accessible && ("getPositionAttribute".equals(name)
                            || "getSizeAttribute".equals(name));
                        boolean nativePaint = widget && "drawRect".equals(name)
                            && "(JJLorg/eclipse/swt/internal/cocoa/NSRect;)V".equals(descriptor);
                        boolean initializePainter = widget && "<clinit>".equals(name);
                        if (!clientArea && !accessibleBounds && !nativePaint && !initializePainter) {
                            return method;
                        }
                        return new MethodVisitor(Opcodes.ASM9, method) {
                            @Override
                            public void visitCode() {
                                super.visitCode();
                                if (nativePaint) {
                                    Label original = new Label();
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, WIDGET_CLASS, "isDrawing", "()Z", false);
                                    super.visitJumpInsn(Opcodes.IFEQ, original);
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    super.visitTypeInsn(Opcodes.INSTANCEOF, CONTROL_CLASS);
                                    super.visitJumpInsn(Opcodes.IFEQ, original);
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    super.visitTypeInsn(Opcodes.CHECKCAST, CONTROL_CLASS);
                                    super.visitFieldInsn(Opcodes.GETFIELD, CONTROL_CLASS, "inCacheDisplayInRect", "Z");
                                    super.visitJumpInsn(Opcodes.IFNE, original);
                                    super.visitVarInsn(Opcodes.LLOAD, 1);
                                    super.visitVarInsn(Opcodes.ALOAD, 5);
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, WIDGET_CLASS, "nativeScalePaint",
                                        "(JLorg/eclipse/swt/internal/cocoa/NSRect;)Z", false);
                                    super.visitJumpInsn(Opcodes.IFEQ, original);
                                    super.visitInsn(Opcodes.RETURN);
                                    super.visitLabel(original);
                                    super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                                    replacements[0]++;
                                }
                            }

                            @Override
                            public void visitInsn(int opcode) {
                                if (initializePainter && opcode == Opcodes.RETURN) {
                                    super.visitTypeInsn(Opcodes.NEW, "java/lang/ThreadLocal");
                                    super.visitInsn(Opcodes.DUP);
                                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/ThreadLocal", "<init>", "()V", false);
                                    super.visitFieldInsn(Opcodes.PUTSTATIC, WIDGET_CLASS, "nativeScalePainting",
                                        "Ljava/lang/ThreadLocal;");
                                    replacements[0]++;
                                }
                                super.visitInsn(opcode);
                            }

                            @Override
                            public void visitFieldInsn(
                                int opcode, @NotNull String owner, @NotNull String fieldName, @NotNull String fieldDescriptor
                            ) {
                                super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                                if (accessibleBounds && opcode == Opcodes.GETFIELD && CONTROL_EVENT_CLASS.equals(owner)
                                    && ("width".equals(fieldName) || "height".equals(fieldName))) {
                                    // accessibility locations are in screen points; custom child sizes must match that space
                                    super.visitInsn(Opcodes.DUP);
                                    super.visitInsn(Opcodes.ICONST_0);
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "max", "(II)I", false);
                                    super.visitInsn(Opcodes.I2D);
                                    super.visitLdcInsn("dbeaver.ui.nativeScale.factor");
                                    super.visitLdcInsn("1");
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty",
                                        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "parseDouble",
                                        "(Ljava/lang/String;)D", false);
                                    super.visitInsn(Opcodes.DCONST_1);
                                    super.visitInsn(Opcodes.DSUB);
                                    super.visitInsn(Opcodes.DMUL);
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "round", "(D)J", false);
                                    super.visitInsn(Opcodes.L2I);
                                    super.visitInsn(Opcodes.IADD);
                                    replacements[0]++;
                                }
                            }

                            @Override
                            public void visitMethodInsn(
                                int opcode,
                                @NotNull String owner,
                                @NotNull String methodName,
                                @NotNull String methodDescriptor,
                                boolean isInterface
                            ) {
                                // SWT shell layouts need the transformed content coordinates, not the physical frame
                                if (clientArea && VIEW_CLASS.equals(owner) && "frame".equals(methodName)
                                    && "()Lorg/eclipse/swt/internal/cocoa/NSRect;".equals(methodDescriptor)) {
                                    methodName = "bounds";
                                    replacements[0]++;
                                }
                                super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                            }
                        };
                    }

                    @Override
                    public void visitEnd() {
                        if (widget && !hasInitializer) {
                            MethodVisitor initializer = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                            initializer.visitCode();
                            initializer.visitTypeInsn(Opcodes.NEW, "java/lang/ThreadLocal");
                            initializer.visitInsn(Opcodes.DUP);
                            initializer.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/ThreadLocal", "<init>", "()V", false);
                            initializer.visitFieldInsn(Opcodes.PUTSTATIC, WIDGET_CLASS, "nativeScalePainting", "Ljava/lang/ThreadLocal;");
                            initializer.visitInsn(Opcodes.RETURN);
                            initializer.visitMaxs(2, 0);
                            initializer.visitEnd();
                            replacements[0]++;
                        }
                        super.visitEnd();
                    }
                }, 0);
                if (widget) {
                    // keep the painter in SWT's loader so Cocoa access needs no extra OSGi dependency
                    try (InputStream input = MacOsScaleAgent.class.getResourceAsStream("/" + PAINTER_CLASS + ".class")) {
                        if (input == null) {
                            throw new IOException("Native painter class is missing");
                        }
                        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                            @NotNull
                            @Override
                            public FieldVisitor visitField(
                                int access, @NotNull String name, @NotNull String descriptor,
                                @Nullable String signature, @Nullable Object value
                            ) {
                                return writer.visitField(access, name, descriptor, signature, value);
                            }

                            @Nullable
                            @Override
                            public MethodVisitor visitMethod(
                                int access, @NotNull String name, @NotNull String descriptor,
                                @Nullable String signature, @Nullable String[] exceptions
                            ) {
                                if ("<init>".equals(name) || "<clinit>".equals(name)) {
                                    return null;
                                }
                                return new MethodVisitor(Opcodes.ASM9,
                                    writer.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    @Override
                                    public void visitFieldInsn(
                                        int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor
                                    ) {
                                        super.visitFieldInsn(opcode, PAINTER_CLASS.equals(owner) ? WIDGET_CLASS : owner,
                                            name, descriptor);
                                    }

                                    @Override
                                    public void visitMethodInsn(
                                        int opcode, @NotNull String owner, @NotNull String name,
                                        @NotNull String descriptor, boolean isInterface
                                    ) {
                                        super.visitMethodInsn(opcode, PAINTER_CLASS.equals(owner) ? WIDGET_CLASS : owner,
                                            name, descriptor, isInterface);
                                    }
                                };
                            }
                        }, 0);
                    } catch (IOException exception) {
                        System.setProperty("dbeaver.ui.nativeScale.error", exception.toString());
                        return null;
                    }
                }
                if (replacements[0] != (shell || widget ? 2 : 5)) {
                    System.setProperty("dbeaver.ui.nativeScale.error", "Unexpected SWT implementation: " + className);
                    return null;
                }
                System.setProperty(shell ? READY_PROPERTY : widget ? "dbeaver.ui.nativeScale.painterReady"
                    : "dbeaver.ui.nativeScale.accessibilityReady", "true");
                return writer.toByteArray();
            }
        });
    }
}
