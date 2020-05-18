/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

/**
 * DB2 Table Status
 * 
 * @author Denis Forveille
 */
public enum DB2TableStatus implements DBPNamedObject {
    C("Set integrity pending", new DBSObjectState("Set Integrity Pending", DBIcon.OVER_ERROR)),

    N("Normal", DBSObjectState.NORMAL),

    X("Inoperative", new DBSObjectState("Inoperative", DBIcon.OVER_ERROR));

    private String name;
    private DBSObjectState state;

    // -----------------
    // Constructor
    // -----------------
    private DB2TableStatus(String name, DBSObjectState state)
    {
        this.name = name;
        this.state = state;
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

    public DBSObjectState getState()
    {
        return state;
    }
}