/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
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

    static final Log log = Log.getLog(JDBCReferenceValueHandler.class);

    public static final JDBCReferenceValueHandler INSTANCE = new JDBCReferenceValueHandler();

    @Override
    public int getFeatures()
    {
        return FEATURE_VIEWER | FEATURE_EDITOR | FEATURE_SHOW_ICON;
    }

    /**
     * NumberFormat is not thread safe thus this method is synchronized.
     */
    @NotNull
    @Override
    public synchronized String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        return DBUtils.getDefaultValueDisplayString(value, format);
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

    @Override
    public Class getValueObjectType()
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