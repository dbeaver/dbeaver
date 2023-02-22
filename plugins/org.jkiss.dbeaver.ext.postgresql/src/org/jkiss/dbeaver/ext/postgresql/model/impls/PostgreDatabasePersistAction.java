/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.impls;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

public class PostgreDatabasePersistAction extends SQLDatabasePersistAction {

    public enum ActionType {
        UNKNOWN,
        DROP,
        CREATE,
        COMMENT
    }
    
    private PostgreProcedure procedure;
    private String title;
    private ActionType actionType;

    public PostgreDatabasePersistAction(@NotNull String title, boolean complex,
            @NotNull PostgreProcedure procedure, ActionType actionType) {
        super(title, procedure.getBody(), complex);
        this.procedure = procedure;
        this.title = title;
        this.actionType = actionType;
    }
       

    @Override
    public void afterExecute(DBCSession session, Throwable error)
            throws DBCException {
        if (ActionType.CREATE.equals(this.actionType)) {
            procedure.setWasCreatedOrReplaced(true);
        }
    }

}
