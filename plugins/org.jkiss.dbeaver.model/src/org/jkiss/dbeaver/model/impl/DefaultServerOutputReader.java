/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCServerOutputReader;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/**
 * Default output reader.
 * Dumps SQL warnings
 */
public class DefaultServerOutputReader implements DBCServerOutputReader
{
    @Override
    public boolean isServerOutputEnabled() {
        return true;
    }

    @Override
    public boolean isAsyncOutputReadSupported() {
        return false;
    }

    @Override
    public void readServerOutput(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @Nullable SQLQueryResult queryResult, @Nullable DBCStatement statement, @NotNull PrintWriter output) throws DBCException {
        if (queryResult != null) {
            dumpWarnings(output, queryResult.getWarnings());
        }
    }

    protected void dumpWarnings(@NotNull PrintWriter output, List<Throwable> warnings) {
        if (warnings != null && warnings.size() > 0) {
            for (Throwable warning : warnings) {
                if (warning instanceof SQLException) {
                    if (false) {
                        // Do not print SQL state. It breaks output.
                        String sqlState = ((SQLException) warning).getSQLState();
                        if (!CommonUtils.isEmpty(sqlState)) {
                            output.print(sqlState + ": ");
                        }
                    }
                }
                output.println(warning.getMessage());
            }
        }
    }

}
