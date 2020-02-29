/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DB2 Column Hidden Status
 * 
 * @author Denis Forveille
 */
public enum DB2ColumnHiddenState implements DBPNamedObject {
    I("Implicitely hidden", false),

    S("System managed hidden", true);

    private String name;
    private Boolean hidden;

    // -----------
    // Constructor
    // -----------

    private DB2ColumnHiddenState(String name, Boolean hidden)
    {
        this.name = name;
        this.hidden = hidden;
    }

    // ----------------
    // Static Helpers
    // ----------------

    public static Boolean isHidden(String hiddenChar)
    {
        if (hiddenChar == null) {
            return false;
        }
        if (hiddenChar.trim().length() == 0) {
            return false;
        }
        return DB2ColumnHiddenState.valueOf(hiddenChar).isHidden();
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

    public Boolean isHidden()
    {
        return hidden;
    }
}