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
import org.jkiss.dbeaver.model.struct.DBSObjectState;

/**
 * DB2 Trigger Valid attribute
 *
 * @author Denis Forveille
 */
public enum DB2TriggerValid implements DBPNamedObject {
    N("Invalid", DBSObjectState.INVALID),

    X("Inoperative", DBSObjectState.INVALID), // DF: No exact correspondance

    Y("Valid", DBSObjectState.ACTIVE); // DF: No exact correspondance

    private final String title;
    private final DBSObjectState state;

    DB2TriggerValid(String title, DBSObjectState state) {
        this.title = title;
        this.state = state;
    }

    @NotNull
    @Override
    public String getName() {
        return title;
    }

    public DBSObjectState getState() {
        return state;
    }
}