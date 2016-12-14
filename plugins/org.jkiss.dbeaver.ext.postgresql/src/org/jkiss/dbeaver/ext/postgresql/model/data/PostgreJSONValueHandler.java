/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;

/**
 * PostgreJSONValueHandler
 */
public class PostgreJSONValueHandler extends JDBCContentValueHandler {

    public static final PostgreJSONValueHandler INSTANCE = new PostgreJSONValueHandler();

    @Override
    protected DBDContent fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws SQLException {
        String json = resultSet.getString(index);
        return new PostgreContentJSON(session.getDataSource(), json);
    }

    @Override
    public DBDContent getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (PostgreUtils.isPGObject(object)) {
            object = PostgreUtils.extractPGObjectValue(object);
        }
        if (object == null) {
            return new PostgreContentJSON(session.getDataSource(), null);
        } else if (object instanceof PostgreContentJSON) {
            return copy ? ((PostgreContentJSON) object).cloneValue(session.getProgressMonitor()) : (PostgreContentJSON) object;
        } else if (object instanceof String) {
            return new PostgreContentJSON(session.getDataSource(), (String) object);
        }
        return super.getValueFromObject(session, type, object, copy);
    }
}
