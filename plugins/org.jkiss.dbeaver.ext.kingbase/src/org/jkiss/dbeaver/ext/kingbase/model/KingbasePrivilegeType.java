/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.kingbase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.access.DBAPrivilegeType;

/**
 * KingbasePrivilegeType
 *
 */
public enum KingbasePrivilegeType implements DBAPrivilegeType {
    ALL(' ', false, Object.class),
    SELECT('r', true, KingbaseTableBase.class, KingbaseTableColumn.class),
    INSERT('a', true, KingbaseTableReal.class, KingbaseTableColumn.class),
    UPDATE('w', true, KingbaseTableBase.class, KingbaseTableColumn.class),
    DELETE('d', true, KingbaseTableReal.class, KingbaseTableColumn.class),
    TRUNCATE('D', true, KingbaseTableReal.class),
    REFERENCES('x', true, KingbaseTableReal.class, KingbaseTableColumn.class),
    TRIGGER('t', true, KingbaseTableReal.class),
    CREATE('C', true, KingbaseDatabase.class, KingbaseSchema.class, KingbaseTablespace.class),
    CONNECT('c', true, KingbaseDatabase.class),
    TEMPORARY('T', true, KingbaseDatabase.class),
    EXECUTE('X', true, KingbaseProcedure.class),
    USAGE('U', true, KingbaseSequence.class, KingbaseDataType.class, KingbaseSchema.class),
    RULE('R', true, KingbaseTableReal.class),
    GRANT('g', true, KingbaseDatabase.class, KingbaseSchema.class, KingbaseTableReal.class, KingbaseDataType.class),
    ZONECONFIG('z', true, KingbaseDatabase.class, KingbaseTableReal.class),
    ALTER('A', true, KingbaseDatabase.class, KingbaseSchema.class, KingbaseTableReal.class), // Redshift-specific

    UNKNOWN((char)0, false);

    private final Class<?>[] targetType;
    private final char code;
    private final boolean valid;

    KingbasePrivilegeType(char code, boolean valid, Class<?> ... targetType) {
        this.code = code;
        this.valid = valid;
        this.targetType = targetType;
    }

    public char getCode() {
        return code;
    }

    public Class<?>[] getTargetType() {
        return targetType;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public boolean supportsType(@NotNull Class<?> objectType) {
        if (KingbaseRole.class.isAssignableFrom(objectType)) {
            return true;
        }
        for (Class<?> aClass : targetType) {
            if (aClass.isAssignableFrom(objectType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return name();
    }

    public static KingbasePrivilegeType fromString(String type) {
        try {
            return valueOf(type);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public static KingbasePrivilegeType getByCode(char pCode) {
        for (KingbasePrivilegeType pt : values()) {
            if (pt.getCode() == pCode) {
                return pt;
            }
        }
        return UNKNOWN;
    }

}

