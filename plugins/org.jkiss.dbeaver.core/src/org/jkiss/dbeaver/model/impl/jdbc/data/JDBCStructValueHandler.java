/*
 * Copyright (C) 2010-2014 Serge Rieder
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandlerStruct;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;

/**
 * JDBC Struct value handler.
 * Handle STRUCT types.
 *
 * @author Serge Rider
 */
public class JDBCStructValueHandler extends JDBCComplexValueHandler implements DBDValueHandlerStruct {

    static final Log log = LogFactory.getLog(JDBCStructValueHandler.class);

    public static final JDBCStructValueHandler INSTANCE = new JDBCStructValueHandler();

    /**
     * NumberFormat is not thread safe thus this method is synchronized.
     */
    @NotNull
    @Override
    public synchronized String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        JDBCStruct struct = (JDBCStruct) value;
        return DBUtils.isNullValue(struct) ?
                DBUtils.getDefaultValueDisplayString(null, format) :
                struct.getStringRepresentation();
    }

    @Override
    public Class getValueObjectType()
    {
        return Struct.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        String typeName;
        try {
            if (object instanceof Struct) {
                typeName = ((Struct) object).getSQLTypeName();
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
                Types.STRUCT,
                typeName,
                "Synthetic struct type for '" + typeName + "'",
                false, false, 0, 0, 0);
        }
        if (object == null) {
            return new JDBCStruct(session, dataType, null);
        } else if (object instanceof JDBCStruct) {
            return copy ? ((JDBCStruct) object).cloneValue(session.getProgressMonitor()) : object;
        } else if (object instanceof Struct) {
            return new JDBCStruct(session, dataType, (Struct) object);
        } else {
            throw new DBCException("Unsupported struct type: " + object.getClass().getName());
        }
    }

    @Nullable
    @Override
    public Object getFieldValue(@NotNull Object owner, @NotNull DBSAttributeBase attribute, int attributeIndex) throws DBCException {
        if (owner instanceof JDBCStruct) {
            try {
                return ((JDBCStruct) owner).getAttributeValue(attribute);
            } catch (Exception e) {
                throw new DBCException("Error reading structure attributes", e);
            }
        } else {
            log.error("Unsupported struct value: " + owner);
            return null;
        }
    }

    @Override
    public void setFieldValue(@NotNull Object owner, @NotNull DBSAttributeBase attribute, int attributeIndex, @Nullable Object value) throws DBCException {
        if (owner instanceof JDBCStruct) {
            try {
                ((JDBCStruct) owner).setAttributeValue(attribute, value);
            } catch (Exception e) {
                log.error("Error setting structure attribute", e);
            }
        } else {
            log.error("Unsupported struct value: " + owner);
        }
    }
}