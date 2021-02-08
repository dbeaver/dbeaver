/*
 * Copyright (C) 2013-2015 Denis Forveille titou10.titou10@gmail.com
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

    private String name;
    private Boolean virtual;

    // -----------------
    // Constructor
    // -----------------
    private DB2IndexColVirtual(String name, Boolean virtual)
    {
        this.name = name;
        this.virtual = virtual;
    }

    // -----------------------
    // Helpers
    // -----------------------

    public Boolean isNotVirtual()
    {
        return !virtual;
    }

    // -----------------------
    // Display @Property Value
    // -----------------------
    @Override
    public String toString()
    {
        return name;
    }

    // ----------------
    // Standard Getters
    // ----------------
    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    public Boolean isVirtual()
    {
        return virtual;
    }

}