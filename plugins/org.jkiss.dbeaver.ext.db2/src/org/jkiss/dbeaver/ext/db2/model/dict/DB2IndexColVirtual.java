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
 * DB2 Index Virtual status
 *
 * @author Denis Forveille
 */
public enum DB2IndexColVirtual implements DBPNamedObject {
    N("", false),

    S("Virtual Index Column", true),

    Y("Virtual Index Column not in this Table", true);

    private final String title;
    private final Boolean virtual;

    DB2IndexColVirtual(String title, Boolean virtual) {
        this.title = title;
        this.virtual = virtual;
    }

    public Boolean isNotVirtual() {
        return !virtual;
    }

    @NotNull
    @Override
    public String getName() {
        return title;
    }

    public Boolean isVirtual() {
        return virtual;
    }

}