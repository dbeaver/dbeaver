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
package org.jkiss.dbeaver.ext.clickhouse.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;

/**
 * Renders ClickHouse {@code JSON} columns through the JSON content viewer/editor.
 * <p>
 * The value is read via {@code getString()}, which yields canonical JSON text when the session runs
 * with {@code output_format_binary_write_json_as_string=1}, and wrapped in a
 * {@link ClickhouseContentJSON} (content type {@code text/json}). Edits are written back as the JSON
 * text (the inherited content binding does {@code setString}); ClickHouse applies it via an
 * {@code ALTER TABLE ... UPDATE} mutation and rejects invalid JSON.
 */
public class ClickhouseJSONValueHandler extends JDBCContentValueHandler {

    public static final ClickhouseJSONValueHandler INSTANCE = new ClickhouseJSONValueHandler();

    @NotNull
    @Override
    protected DBDContent fetchColumnValue(
        @NotNull DBCSession session,
        @NotNull JDBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index
    ) throws SQLException {
        // getString() yields canonical JSON when the session enables output_format_binary_write_json_as_string;
        // a SQL NULL comes back as a null string, which ClickhouseContentJSON represents as an SQL NULL value.
        return new ClickhouseContentJSON(session.getExecutionContext(), resultSet.getString(index));
    }

    @NotNull
    @Override
    public DBDContent getValueFromObject(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        @Nullable Object object,
        boolean copy,
        boolean validateValue
    ) throws DBCException {
        return switch (object) {
            case null -> new ClickhouseContentJSON(session.getExecutionContext(), null);
            case String stringValue -> new ClickhouseContentJSON(session.getExecutionContext(), stringValue);
            // An existing ClickhouseContentJSON (or any other DBDContent) is handled by the superclass,
            // which returns it as-is and clones it via cloneValue() when a copy is requested.
            default -> super.getValueFromObject(session, type, object, copy, validateValue);
        };
    }
}
