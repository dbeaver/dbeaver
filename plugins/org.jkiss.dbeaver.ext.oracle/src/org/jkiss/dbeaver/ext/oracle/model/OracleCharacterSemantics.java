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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.Nullable;

/**
 * Oracle column length semantics (CHAR_USED values).
 */
public enum OracleCharacterSemantics {

    BYTE("B", "BYTE"),
    CHAR("C", "CHAR");

    private final String code;
    private final String keyword;

    OracleCharacterSemantics(String code, String keyword) {
        this.code = code;
        this.keyword = keyword;
    }

    public String getCode() {
        return code;
    }

    public String getKeyword() {
        return keyword;
    }

    @Nullable
    public static OracleCharacterSemantics resolve(@Nullable String code) {
        if (code == null) {
            return null;
        }
        for (OracleCharacterSemantics value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
