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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * BFILE type support
 */
public class OracleBFILEValueHandler extends JDBCContentValueHandler {

    public static final OracleBFILEValueHandler INSTANCE = new OracleBFILEValueHandler();

    @Override
    protected DBDContent fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException
    {
        Object object;

        try {
            object = resultSet.getObject(index);
        } catch (SQLException e) {
            object = null;
        }

        if (object == null) {
            return new OracleContentBFILE(session.getDataSource(), null);
        } else {
            return new OracleContentBFILE(session.getDataSource(), object);
        }
    }

    @Override
    public DBDContent getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return new OracleContentBFILE(session.getDataSource(), null);
        } else if (object instanceof OracleContentBFILE) {
            return copy ? (OracleContentBFILE)((OracleContentBFILE) object).cloneValue(session.getProgressMonitor()) : (OracleContentBFILE) object;
        }
        return super.getValueFromObject(session, type, object, copy);
    }

}
