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
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DB2 Routine Origin
 *
 * @author Denis Forveille
 */
public enum DB2RoutineOrigin implements DBPNamedObject {
    A("User-defined aggregate interface function"),

    B("Built-in)"),

    E("User-defined, external"),

    M("Template function"),

    F("Federated procedure"),

    Q("SQL-bodied"),

    R("System-generated SQL-bodied routine"),

    S("System-generated"),

    T("System-generated transform function (not directly invokable)"),

    U("User-defined, based on a source");

    private final String title;

    DB2RoutineOrigin(String title) {
        this.title = title;
    }

    @NotNull
    @Override
    public String getName() {
        return title;
    }

}