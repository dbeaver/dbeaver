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
package org.jkiss.dbeaver.ext.postgresql.model;

import java.util.Arrays;

/**
 * PostgreAttributeStorage
 */
public enum PostgreAttributeStorage {
    DEFAULT("?"),
    PLAIN("p"),
    MAIN("m"),
    EXTERNAL("e"),
    EXTENDED("x");

    private final String code;

    PostgreAttributeStorage(String code) {
        this.code = code;
    }

    protected boolean isSupported(PostgreDataSource dataSource) {
        return (! this.code.equals("?")) || dataSource.isServerVersionAtLeast(16, 0);
    }

    public String getCode() {
        return code;
    }

    public static PostgreAttributeStorage getByCode(String code) {
        for (PostgreAttributeStorage as : values()) {
            if (as.getCode().equals(code)) {
                return as;
            }
        }
        return DEFAULT;
    }

    public static PostgreAttributeStorage[] getValues(PostgreDataSource dataSource) {
        return Arrays.stream(values())
            .filter(e -> e.isSupported(dataSource))
            .toArray(PostgreAttributeStorage[]::new);
    } 
}
