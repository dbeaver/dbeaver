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
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCReference;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Ref;
import java.sql.SQLException;
import java.sql.Types;

/**
 * JDBC reference value handler.
 * Handle STRUCT types.
 *
 * @author Serge Rider
 */
public class JDBCReferenceValueHandler extends JDBCComplexValueHandler {

    private static final Log log = Log.getLog(JDBCReferenceValueHandler.class);

    public static final JDBCReferenceValueHandler INSTANCE = new JDBCReferenceValueHandler();

    /**
     * NumberFormat is not thread safe thus this method is synchronized.
     */
    @NotNull
    @Override
    public synchronized String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        return DBValueFormatting.getDefaultValueDisplayString(value, format);
    }

    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        JDBCReference reference = (JDBCReference) value;
        statement.setRef(paramIndex, reference.getValue());
    }

    @NotNull
    @Override
    public Class<Ref> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return Ref.class;
    }

    @Override
    public JDBCReference getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        String typeName;
        try {
            if (object instanceof Ref) {
                typeName = ((Ref) object).getBaseTypeName();
            } else {
                typeName = type.getTypeName();
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
        DBSDataType dataType = null;
        try {
            dataType = DBUtils.resolveDataType(session.getProgressMonitor(), session.getDataSource(), typeName);
        } catch (DBException e) {
            log.error("Error resolving data type '" + typeName + "'", e);
        }
        if (dataType == null) {
            dataType = new JDBCDataType(
                session.getDataSource().getContainer(),
                Types.REF,
                typeName,
                "Synthetic struct type for reference '" + typeName + "'",
                false, false, 0, 0, 0);
        }
        if (object == null) {
            return new JDBCReference(dataType, null);
        } else if (object instanceof JDBCReference) {
            return (JDBCReference)object;
        } else if (object instanceof Ref) {
            return new JDBCReference(dataType, (Ref) object);
        } else {
            throw new DBCException("Unsupported struct type: " + object.getClass().getName());
        }
    }

}