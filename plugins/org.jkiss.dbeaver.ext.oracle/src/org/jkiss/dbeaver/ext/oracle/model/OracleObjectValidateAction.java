/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
    public void handleExecute(DBCSession session, Throwable error) throws DBCException {
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
