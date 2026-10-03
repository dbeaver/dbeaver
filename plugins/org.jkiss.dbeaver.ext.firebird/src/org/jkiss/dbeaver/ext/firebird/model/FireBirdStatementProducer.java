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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCException;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryParameter;

import java.sql.SQLException;
import java.sql.Types;

final class FireBirdStatementProducer implements DBCStatementProducer {

    @Override
    public boolean useAdapter(@NotNull SQLQuery query) {
        return query.getParameters() != null && query.getParameters().stream().anyMatch(SQLQueryParameter::isNativeBinding);
    }

    @NotNull
    @Override
    public DBCStatement createStatement(
        @Nullable DBCExecutionSource source, @NotNull DBCSession session, @NotNull SQLQuery query,
        @NotNull String queryText, boolean scrollable
    ) throws DBCException {
        DBCStatement statement = DBUtils.makeStatement(session, queryText, scrollable);
        try {
            statement.setStatementSource(source);
            if (!(statement instanceof JDBCPreparedStatement prepared)) {
                throw new DBCException("Firebird EXECUTE BLOCK requires a JDBC prepared statement");
            }
            if (query.getParameters() == null) {
                return statement;
            }
            int index = 1;
            for (SQLQueryParameter parameter : query.getParameters()) {
                if (!parameter.isNativeBinding()) {
                    continue;
                }
                String value = parameter.getValue();
                if (value == null || value.isEmpty() || SQLConstants.NULL_VALUE.equalsIgnoreCase(value)) {
                    prepared.setNull(index++, Types.NULL);
                } else {
                    prepared.setString(index++, value);
                }
            }
            return statement;
        } catch (SQLException e) {
            closeOnError(statement, e);
            throw new JDBCException(e, session.getExecutionContext());
        } catch (DBCException e) {
            closeOnError(statement, e);
            throw e;
        }
    }

    private static void closeOnError(@NotNull DBCStatement statement, @NotNull Exception error) {
        try {
            statement.close();
        } catch (DBException closeError) {
            error.addSuppressed(closeError);
        }
    }
}
