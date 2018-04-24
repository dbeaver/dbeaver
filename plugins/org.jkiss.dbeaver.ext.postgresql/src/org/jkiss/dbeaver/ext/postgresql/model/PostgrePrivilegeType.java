/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

/**
 * PostgrePrivilegeType
 */
public enum PostgrePrivilegeType {
    // ALL privs
    ALL(Object.class, ' ', false),
    // TABLE privs
    SELECT(PostgreTableBase.class, 'r', true),
    INSERT(PostgreTableReal.class, 'a', true),
    UPDATE(PostgreTableBase.class, 'w', true),
    DELETE(PostgreTableReal.class, 'd', true),
    TRUNCATE(PostgreTableReal.class, 't', true),
    REFERENCES(PostgreTableReal.class, 'x', true),
    TRIGGER(PostgreTableReal.class, 'D', true),
    // SEQUENCE privs
    USAGE(PostgreSequence.class, 'U', true),

    EXECUTE(PostgreProcedure.class, 'X', true),

    UNKNOWN(Object.class, (char)0, false);

    private final Class<?> targetType;
    private final char code;
    private final boolean valid;

    PostgrePrivilegeType(Class<?> targetType, char code, boolean valid) {
        this.targetType = targetType;
        this.code = code;
        this.valid = valid;
    }

    public char getCode() {
        return code;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    public boolean isValid() {
        return valid;
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

