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
 * DB2 View Check Status
 *
 * @author Denis Forveille
 */
public enum DB2ViewCheck implements DBPNamedObject {
    C("Cascaded check"),

    L("Local check"),

    N("No check");

    private final String title;

    DB2ViewCheck(String title) {
        this.title = title;
    }

    @NotNull
    @Override
    public String getName() {
        return title;
    }

}