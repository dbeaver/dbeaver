package org.jkiss.dbeaver.antlr.model.internal;

import java.util.ArrayList;
import java.util.Map;

public enum FieldTypeKind {
    String(true), 
    Byte(true),
    Short(true), 
    Int(true),
    Long(true),
    Bool(true),
    Float(true),
    Double(true),
    Enum(true),
    Object(false),
    Array(false),
    List(false);
    
    public final boolean isTerm;
    
    FieldTypeKind(boolean isTerm) {
        this.isTerm = isTerm;
    }

    private static final Map<Class<?>, FieldTypeKind> builtinTypeKinds = Map.ofEntries(
        java.util.Map.entry(String.class, FieldTypeKind.String), 
        java.util.Map.entry(Byte.class, FieldTypeKind.Byte),
        java.util.Map.entry(Short.class, FieldTypeKind.Short), 
        java.util.Map.entry(Integer.class, FieldTypeKind.Int), 
        java.util.Map.entry(Long.class, FieldTypeKind.Long),
        java.util.Map.entry(Boolean.class, FieldTypeKind.Bool), 
        java.util.Map.entry(Float.class, FieldTypeKind.Float),
        java.util.Map.entry(Double.class, FieldTypeKind.Double), 

        java.util.Map.entry(byte.class, FieldTypeKind.Byte),
        java.util.Map.entry(short.class, FieldTypeKind.Short), 
        java.util.Map.entry(int.class, FieldTypeKind.Int),
        java.util.Map.entry(long.class, FieldTypeKind.Long), 
        java.util.Map.entry(boolean.class, FieldTypeKind.Bool), 
        java.util.Map.entry(float.class, FieldTypeKind.Float),
        java.util.Map.entry(double.class, FieldTypeKind.Double) 
    );

    public static FieldTypeKind resolveModelFieldKind(Class<?> fieldType) {
        FieldTypeKind kind = builtinTypeKinds.get(fieldType);
        if (kind == null) {
            if (fieldType.isEnum()) {
                kind = FieldTypeKind.Enum;
            } else if (fieldType.isArray()) {
                kind = FieldTypeKind.Array;
            } else if (fieldType.isAssignableFrom(ArrayList.class)) {
                kind = FieldTypeKind.List; 
            } else {
                kind = FieldTypeKind.Object;
            }
        }
        return kind;
    }
}

