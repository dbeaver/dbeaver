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

import java.io.Reader;
import java.sql.SQLException;

/**
 * The {@code CLOB}/{@code NCLOB} sibling of {@link MimerBLOBValueHandler}: the same driver
 * limitation applies to all three Mimer SQL LOB types - {@code getObject()}/{@code getClob()}/
 * {@code getString()} don't work, use {@code getCharacterStream()} instead. One handler covers
 * both {@code CLOB} and {@code NCLOB}.
 *
 * @author Mimer Information Technology
 */
public class MimerCLOBValueHandler extends JDBCContentValueHandler {

    public static final MimerCLOBValueHandler INSTANCE = new MimerCLOBValueHandler();

    @Override
    protected DBDContent fetchColumnValue(
        @NotNull DBCSession session,
        @NotNull JDBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index
    ) throws DBCException, SQLException {
        Reader reader = resultSet.getCharacterStream(index);
        return getValueFromObject(session, type, reader, false, false);
    }
}
