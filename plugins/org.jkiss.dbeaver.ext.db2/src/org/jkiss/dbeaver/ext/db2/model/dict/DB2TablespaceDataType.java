/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
 * Type of data that can be stored in a DB2 Tablespace
 * 
 * @author Denis Forveille
 */
public enum DB2TablespaceDataType implements DBPNamedObject {
    A("Regular", true),

    L("Large", true),

    T("System temporary tables only", false),

    U("Created temporary tables or declared temporary tables only", false);

    private String name;
    private Boolean validForUserTables;

    // -----------------
    // Constructor
    // -----------------
    private DB2TablespaceDataType(String name, Boolean validForUserTables)
    {
        this.name = name;
        this.validForUserTables = validForUserTables;
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

    public Boolean isValidForUserTables()
    {
        return validForUserTables;
    }
}