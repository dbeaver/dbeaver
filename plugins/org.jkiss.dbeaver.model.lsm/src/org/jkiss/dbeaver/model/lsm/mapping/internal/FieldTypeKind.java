/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.lsm.mapping.internal;

import org.jkiss.code.NotNull;

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
    LiteralList(true),
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

    @NotNull
    public static FieldTypeKind resolveModelLiteralFieldKind(@NotNull Class<?> fieldType) {
        FieldTypeKind kind = builtinTypeKinds.get(fieldType);
        if (kind == null) {
            if (fieldType.isEnum()) {
                kind = FieldTypeKind.Enum;
            } else if (fieldType.isAssignableFrom(ArrayList.class)) {
                kind = LiteralList;
            } else {
                kind = FieldTypeKind.String;
            }
        }
        return kind;
    }

    @NotNull
    public static FieldTypeKind resolveModelSubnodeFieldKind(@NotNull Class<?> fieldType) {
        FieldTypeKind kind;
        if (fieldType.isArray()) {
            kind = FieldTypeKind.Array;
        } else if (fieldType.isAssignableFrom(ArrayList.class)) {
            kind = FieldTypeKind.List; 
        } else {
            kind = FieldTypeKind.Object;
        }
        return kind;
    }
}

