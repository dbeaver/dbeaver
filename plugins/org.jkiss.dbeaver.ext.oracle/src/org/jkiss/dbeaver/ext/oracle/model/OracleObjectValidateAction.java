/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.ext.oracle.actions.CompileHandler;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileError;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLog;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLogBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

/**
 * Oracle persist action with validation
 */
public class OracleObjectValidateAction extends OracleObjectPersistAction {

    private final OracleSourceObject object;

    public OracleObjectValidateAction(OracleSourceObject object, OracleObjectType objectType, String title, String script) {
        super(objectType, title, script);
        this.object = object;
    }

    @Override
    public void afterExecute(DBCSession session, Throwable error) throws DBCException {
        if (error != null) {
            return;
        }
        DBCCompileLog log = new DBCCompileLogBase();
        CompileHandler.logObjectErrors((JDBCSession) session, log, object, getObjectType());
        if (!log.getErrorStack().isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("Error during ").append(getObjectType().getTypeName()).append(" '").append(object.getName()).append("' validation:");
            for (DBCCompileError e : log.getErrorStack()) {
                message.append("\n");
                message.append(e.toString());
            }
            throw new DBCException(message.toString());
        }
    }
}
