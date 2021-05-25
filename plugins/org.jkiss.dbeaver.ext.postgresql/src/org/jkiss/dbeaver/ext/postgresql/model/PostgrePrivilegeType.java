/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.model.access.DBAPrivilegeType;

/**
 * PostgrePrivilegeType
 *
 * @see <a href="https://www.postgresql.org/docs/current/ddl-priv.html#PRIVILEGE-ABBREVS-TABLE">5.1. ACL Privilege Abbreviations</a>
 */
public enum PostgrePrivilegeType implements DBAPrivilegeType {
    // ALL privs
    ALL(' ', false, Object.class),
    // TABLE privs
    SELECT('r', true, PostgreTableBase.class, PostgreTableColumn.class),
    INSERT('a', true, PostgreTableReal.class, PostgreTableColumn.class),
    UPDATE('w', true, PostgreTableBase.class, PostgreTableColumn.class),
    DELETE('d', true, PostgreTableReal.class, PostgreTableColumn.class),
    TRUNCATE('D', true, PostgreTableReal.class),
    REFERENCES('x', true, PostgreTableReal.class, PostgreTableColumn.class),
    TRIGGER('t', true, PostgreTableReal.class),
    CREATE('C', true, PostgreDatabase.class, PostgreSchema.class, PostgreTablespace.class),
    CONNECT('c', true, PostgreDatabase.class),
    TEMPORARY('T', true, PostgreDatabase.class),
    EXECUTE('X', true, PostgreProcedure.class),
    USAGE('U', true, PostgreSequence.class, PostgreDataType.class, PostgreSchema.class),

    // Redshift-specific types
    RULE('R', true, PostgreTableReal.class),

    UNKNOWN((char)0, false);

    private final Class<?>[] targetType;
    private final char code;
    private final boolean valid;

    PostgrePrivilegeType(char code, boolean valid, Class<?> ... targetType) {
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
    public boolean supportsType(Class<?> objectType) {
        if (PostgreRole.class.isAssignableFrom(objectType)) {
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

    public static PostgrePrivilegeType fromString(String type) {
        try {
            return valueOf(type);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public static PostgrePrivilegeType getByCode(char pCode) {
        for (PostgrePrivilegeType pt : values()) {
            if (pt.getCode() == pCode) {
                return pt;
            }
        }
        return UNKNOWN;
    }

}

