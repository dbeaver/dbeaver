/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
 * PostgreTablePersistence
 */
public enum PostgreTablePersistence {
    PERMANENT('p', "TABLE"),
    UNLOGGED('u', "UNLOGGED TABLE"),
    TEMPORARY('t', "TEMP TABLE"),
    UNKNOWN('?', "?");

    private final char code;
    private final String tableType;

    PostgreTablePersistence(char code, String tableType) {
        this.code = code;
        this.tableType = tableType;
    }

    public char getCode() {
        return code;
    }

    public static PostgreTablePersistence getByCode(String code) {
        return code != null && code.length() == 1 ? getByCode(code.charAt(0)) : UNKNOWN;
    }

    public static PostgreTablePersistence getByCode(char pCode) {
        for (PostgreTablePersistence pt : values()) {
            if (pt.getCode() == pCode) {
                return pt;
            }
        }
        return UNKNOWN;
    }

    public String getTableTypeClause() {
        return tableType;
    }
}

