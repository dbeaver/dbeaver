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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCServerOutputReader;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Default output reader.
 * Dumps SQL warnings
 */
public class DefaultServerOutputReader implements DBCServerOutputReader
{
    private final SQLQueryResult queryResult;

    public DefaultServerOutputReader(SQLQueryResult queryResult) {
        this.queryResult = queryResult;
    }

    @Override
    public boolean isServerOutputEnabled() {
        return true;
    }

    @Override
    public void enableServerOutput(DBRProgressMonitor monitor, DBCExecutionContext context, boolean enable) throws DBCException {
        // do nothing
    }

    @Override
    public void readServerOutput(DBRProgressMonitor monitor, DBCExecutionContext context, PrintWriter output) throws DBCException {
        Throwable[] warnings = queryResult.getWarnings();
        if (warnings != null && warnings.length > 0) {
            for (Throwable warning : warnings) {
                if (warning instanceof SQLException) {
                    String sqlState = ((SQLException) warning).getSQLState();
                    if (!CommonUtils.isEmpty(sqlState)) {
                        output.print(sqlState + ": ");
                    }
                }
                output.println(warning.getMessage());
            }
        }
    }

}
