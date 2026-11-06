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
package org.jkiss.dbeaver.ext.mimer.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.io.InputStream;
import java.sql.SQLException;

/**
 * Works around a Mimer SQL JDBC driver limitation: {@code getObject()}/{@code getBlob()}/{@code
 * getBytes()} don't work for a {@code BLOB} column - use {@code getBinaryStream()} instead.
 * Overrides {@code fetchColumnValue} to do exactly that, relying on {@link
 * JDBCContentValueHandler#getValueFromObject}'s existing {@code InputStream} handling for the
 * rest.
 *
 * @author Mimer Information Technology
 */
public class MimerBLOBValueHandler extends JDBCContentValueHandler {

    public static final MimerBLOBValueHandler INSTANCE = new MimerBLOBValueHandler();

    @Override
    protected DBDContent fetchColumnValue(
        @NotNull DBCSession session,
        @NotNull JDBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index
    ) throws DBCException, SQLException {
        InputStream stream = resultSet.getBinaryStream(index);
        return getValueFromObject(session, type, stream, false, false);
    }
}
