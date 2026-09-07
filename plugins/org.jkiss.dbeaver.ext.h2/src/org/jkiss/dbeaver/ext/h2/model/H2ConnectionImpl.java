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
package org.jkiss.dbeaver.ext.h2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.h2.util.H2Utils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCObjectSupplier;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCConnectionImpl;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class H2ConnectionImpl extends JDBCConnectionImpl {
    public H2ConnectionImpl(
        @NotNull JDBCExecutionContext context,
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionPurpose purpose,
        @NotNull String taskTitle
    ) {
        super(context, monitor, purpose, taskTitle);
    }

    @NotNull
    @Override
    public JDBCStatement prepareStatement(
        @NotNull DBCStatementType type,
        @NotNull String sqlQuery,
        boolean scrollable,
        boolean updatable,
        boolean returnGeneratedKeys
    ) throws DBCException {
        try {
            validateQuery(sqlQuery);
        } catch (SQLException e) {
            throw new DBCException(
                e.getMessage(),
                e,
                getExecutionContext()
            );
        }
        return super.prepareStatement(type, sqlQuery, scrollable, updatable, returnGeneratedKeys);
    }

    @Override
    protected JDBCStatement createStatementImpl(@NotNull JDBCObjectSupplier<Statement> original) throws SQLException {
        return new H2StatementImpl(this, original, !isLoggingEnabled());
    }

    @Override
    protected JDBCPreparedStatement createPreparedStatementImpl(
        @NotNull JDBCObjectSupplier<PreparedStatement> statementSupplier,
        @Nullable String sql
    ) throws SQLException {
        validateQuery(sql);
        return super.createPreparedStatementImpl(statementSupplier, sql);
    }

    @Override
    protected JDBCCallableStatement createCallableStatementImpl(
        @NotNull JDBCObjectSupplier<CallableStatement> statementSupplier,
        @Nullable String sql
    ) throws SQLException {
        validateQuery(sql);
        return super.createCallableStatementImpl(statementSupplier, sql);
    }

    static void validateQuery(@Nullable String query) throws SQLException {
        if (query != null && H2Utils.isClassLoadingRestricted() && H2Utils.isJavaSourceDefinition(query)) {
            throw new SQLException(
                "Java source aliases and triggers are disabled for embedded H2 databases. " +
                "Allow all H2 classes in Preferences > Drivers > H2 and restart DBeaver to enable them"
            );
        }
    }
}
