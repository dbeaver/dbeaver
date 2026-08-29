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
package org.jkiss.dbeaver.ext.duckdb.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.duckdb.model.DuckDBDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;

/**
 * Tests whether the active result set belongs to a DuckDB connection.
 * Used to show the "Export to Parquet file" command only where DuckDB's COPY is available.
 */
public class DuckDBResultSetPropertyTester extends PropertyTester {

    public static final String NAMESPACE = "org.jkiss.dbeaver.ext.duckdb.ui";
    public static final String PROP_IS_DUCKDB_RESULT_SET = "isDuckDBResultSet";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof IWorkbenchPart part)) {
            return false;
        }
        if (PROP_IS_DUCKDB_RESULT_SET.equals(property)) {
            IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(part);
            if (resultSet == null) {
                return false;
            }
            DBCExecutionContext context = resultSet.getExecutionContext();
            return context != null && context.getDataSource() instanceof DuckDBDataSource;
        }
        return false;
    }
}
