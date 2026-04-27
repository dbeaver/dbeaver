/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentCLOB;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentChars;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;

/**
 * Handles both Altibase CLOB and JSON data types.
 * <p>
 * JSON is internally implemented as a CLOB-based type in Altibase, and the
 * JDBC driver reports JSON columns as Types.CLOB in ResultSetMetaData.
 * Therefore, both types are routed to this handler.
 */
public class AltibaseCLOBValueHandler extends JDBCContentValueHandler {

    private static final AltibaseCLOBValueHandler INSTANCE = new AltibaseCLOBValueHandler();

    /*
    * Altibase stores LOB data in two ways depending on LOB_CACHE_THRESHOLD:
    * - In-row LOB  (len < threshold): Stored directly in the row, no LOB locator needed.
    *                                  Accessible regardless of autocommit mode.
    * - Out-of-row LOB (len >= threshold): Stored via a LOB locator, valid only within a transaction.
    *                                      Requires 'Manual Commit' mode in DBeaver to read.
    * In Autocommit ON mode, the LOB locator is invalidated immediately after query execution, causing:
    *   - ERR-11104 (69828): LobLocator cannot span the transaction.
    *   - ERR-C1013 (201915): Failed to seek a Temporary LOB.
    * In such cases, an informational message is returned to guide the user.
    * Note: getString() is used first to safely fetch in-row LOB data without a LOB locator.
    *
    * LOB_CACHE_THRESHOLD: SELECT VALUE1 FROM V$PROPERTY WHERE NAME = 'LOB_CACHE_THRESHOLD'
    */
    @Nullable
    @Override
    protected DBDContent fetchColumnValue(
            DBCSession session,
            JDBCResultSet resultSet,
            DBSTypedObject type,
            int index) throws DBCException, SQLException {
        try {
            /* [CRITICAL] Do not use getClob() first if Autocommit is ON.
            * Altibase invalidates the LOB Locator immediately upon query completion in Autocommit mode.
            * We use getString() to safely fetch In-row LOB data.
            */
            String value = resultSet.getString(index);

            if (resultSet.wasNull() || value == null) {
                return null;
            }

            long len = (long) value.length();
            long lobCacheThreshold4Char = ((AltibaseDataSource) session.getDataSource()).getLobCacheThreshold4Char();

            if (len < lobCacheThreshold4Char) {
                // In-row LOB
                return new JDBCContentChars(session.getExecutionContext(), value);
            } else {
                /* Out-of-row LOBs are delegated to super.
                In Autocommit ON mode, this may raise a 'LobLocator cannot span' error,
                which is handled in the catch block below.
                */
                return super.fetchColumnValue(session, resultSet, type, index);
            }
        } catch (SQLException e) {
            // Catch Altibase-specific LOB transaction errors
            if (e.getErrorCode() == AltibaseConstants.ERR_LOB_LOCATOR_SPAN_TRANS ||
                e.getErrorCode() == AltibaseConstants.ERR_TEMP_LOB_SEEK_FAILED) {
                return new JDBCContentChars(session.getExecutionContext(),
                        "[Long CLOB/JSON data requires 'Manual Commit' mode in DBeaver to read.]");
            }
            throw e;
        }
    }

    @Override
    protected void bindParameter(
            @NotNull JDBCSession session,
            @NotNull JDBCPreparedStatement statement,
            @NotNull DBSTypedObject paramType,
            int paramIndex,
            @Nullable Object value
    ) throws DBCException, SQLException {
        if (DBUtils.isNullValue(value)) {
            statement.setNull(paramIndex, paramType.getTypeID(), paramType.getTypeName());
        } else if (value instanceof JDBCContentChars contentChars) {
            bindCharParameter(session, statement, paramType, paramIndex, contentChars);
        } else if (value instanceof JDBCContentCLOB contentCLOB) {
            contentCLOB.bindParameter(session, statement, paramType, paramIndex);
        } else {
            throw new DBCException(ModelMessages.model_jdbc_unsupported_value_type_ + value);
        }
    }

    /*
    * If the content fits within the threshold, it is bound as a String for efficiency.
    * Otherwise, the Reader is reset and passed directly as a character stream.
    */
    private void bindCharParameter(
            @NotNull JDBCSession session,
            @NotNull JDBCPreparedStatement statement,
            @NotNull DBSTypedObject paramType,
            int paramIndex,
            @NotNull JDBCContentChars contentChars
    ) throws DBCException, SQLException {
        try {
            Reader reader = contentChars.getContentReader();
            if (reader == null) {
                statement.setNull(paramIndex, paramType.getTypeID(), paramType.getTypeName());
                return;
            }

            long threshold = ((AltibaseDataSource) session.getDataSource()).getLobCacheThreshold4Char();
            long len = contentChars.getContentLength();

            if (len >= 0 && len <= threshold) {
                statement.setString(paramIndex, CommonUtils.toString(contentChars.getRawValue()));
            } else {
                statement.setCharacterStream(paramIndex, reader);
            }
        } catch (IOException e) {
            throw new DBCException("Failed to read CLOB content", e);
        }
    }
}