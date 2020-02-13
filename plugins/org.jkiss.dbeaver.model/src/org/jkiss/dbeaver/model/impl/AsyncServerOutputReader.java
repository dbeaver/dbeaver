/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;

import java.io.PrintWriter;
import java.util.Arrays;

public class AsyncServerOutputReader extends DefaultServerOutputReader {
    private static final Log log = Log.getLog(AsyncServerOutputReader.class);

        @Override
        public boolean isAsyncOutputReadSupported() {
            return true;
        }

        @Override
        public void readServerOutput(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, SQLQueryResult queryResult, DBCStatement statement, @NotNull PrintWriter output) throws DBCException {
            if (statement == null) {
                super.readServerOutput(monitor, context, queryResult, null, output);
            } else {
                // Do not read from connection warnings as it blocks statements cancelation and other connection-level stuff.
                // See #7885
/*
                try {
                    SQLWarning connWarning = ((JDBCSession) statement.getSession()).getWarnings();
                    if (connWarning != null) {
                        dumpWarnings(output, Collections.singletonList(connWarning));
                    }
                } catch (SQLException e) {
                    log.debug(e);
                }
*/

                Throwable[] statementWarnings = statement.getStatementWarnings();
                if (statementWarnings != null && statementWarnings.length > 0) {
                    dumpWarnings(output, Arrays.asList(statementWarnings));
                }
            }
        }
    }

