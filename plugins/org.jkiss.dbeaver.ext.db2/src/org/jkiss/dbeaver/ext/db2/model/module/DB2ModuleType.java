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
package org.jkiss.dbeaver.ext.db2.model.module;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DB2 Type of Module
 *
 * @author Denis Forveille
 */
public enum DB2ModuleType implements DBPNamedObject {
    A("Alias"),

    M("Module"),

    P("PL/SQL");

    private final String name;

    DB2ModuleType(String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

}