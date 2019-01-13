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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentChars;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.sql.SQLException;
import java.sql.Types;

/**
 * JSON content
 */
public class PostgreContentJSON extends JDBCContentChars {

    public PostgreContentJSON(DBPDataSource dataSource, String json)
    {
        super(dataSource, json);
    }

    private PostgreContentJSON(PostgreContentJSON copyFrom) {
        super(copyFrom);
    }

    @NotNull
    @Override
    public String getContentType()
    {
        return MimeTypes.TEXT_JSON;
    }

    @Override
    public void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement preparedStatement,
        DBSTypedObject columnType,
        int paramIndex)
        throws DBCException
    {
        try {
            if (data != null) {
                preparedStatement.setObject(paramIndex, data, Types.OTHER);
            } else {
                preparedStatement.setNull(paramIndex, columnType.getTypeID());
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    @Override
    public String getDisplayString(DBDDisplayFormat format) {
        return data == null ? null : TextUtils.compactWhiteSpaces(data);
    }

    @Override
    public PostgreContentJSON cloneValue(DBRProgressMonitor monitor)
    {
        return new PostgreContentJSON(this);
    }

}
