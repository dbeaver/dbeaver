/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

    @Override
    public void close() {

    }
}
