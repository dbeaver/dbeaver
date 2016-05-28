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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCArrayImpl;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetImpl;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Array holder
 */
public class JDBCCollection implements DBDCollection, DBDValueCloneable {

    private static final Log log = Log.getLog(JDBCCollection.class);

    private Object[] contents;
    private final DBSDataType type;
    private final DBDValueHandler valueHandler;

    @NotNull
    public static Object makeArray(JDBCSession session, DBSTypedObject column, Array array) throws DBCException {
        DBSDataType type = null;
        if (column instanceof DBSTypedObjectEx) {
            type = ((DBSTypedObjectEx) column).getDataType();
        }
        if (type == null) {
            try {
                if (array == null) {
                    String arrayTypeName = column.getTypeName();
                    final DBSDataType arrayType = session.getDataSource().resolveDataType(session.getProgressMonitor(), arrayTypeName);
                    if (arrayType != null) {
                        type = arrayType.getComponentType(session.getProgressMonitor());
                    }
                } else {
                    String baseTypeName = array.getBaseTypeName();
                    type = session.getDataSource().resolveDataType(session.getProgressMonitor(), baseTypeName);
                }
            } catch (Exception e) {
                throw new DBCException("Error resolving data type", e);
            }
        }

        try {
            if (type == null) {
                if (array == null) {
                    throw new DBCException("Can't resolve NULL array data type");
                }
                try {
                    return extractDataFromResultSet(session, array, null, null);
                } catch (SQLException e) {
                    throw new DBCException(e, session.getDataSource()); //$NON-NLS-1$
                }
            }
            DBDValueHandler valueHandler = DBUtils.findValueHandler(session, type);
            try {
                return extractDataFromArray(session, array, type, valueHandler);
            } catch (SQLException e) {
                if (array == null) {
                    throw new DBCException(e, session.getDataSource()); //$NON-NLS-1$
                }
                try {
                    return extractDataFromResultSet(session, array, type, valueHandler);
                } catch (SQLException e1) {
                    throw new DBCException(e1, session.getDataSource()); //$NON-NLS-1$
                }
            }
        } catch (DBException e) {
            throw new DBCException("Can't extract array data from JDBC array", e); //$NON-NLS-1$
        }
    }

    @NotNull
    private static JDBCCollection extractDataFromResultSet(@NotNull JDBCSession session, @NotNull Array array, @Nullable DBSDataType type, @Nullable DBDValueHandler valueHandler) throws SQLException, DBException {
        ResultSet dbResult = array.getResultSet();
        if (dbResult == null) {
            throw new DBCException("JDBC array type was not resolved and result set was not provided by driver. Return NULL.");
        }
        if (type == null || valueHandler == null) {
            JDBCColumnMetaData itemMeta = new JDBCColumnMetaData(session.getDataSource(), dbResult.getMetaData(), 1);
            if (type == null) {
                type = DBUtils.resolveDataType(session.getProgressMonitor(), session.getDataSource(), itemMeta.getTypeName());
                if (type == null) {
                    type = new JDBCDataType(session.getDataSource(), itemMeta);
                }
            }
            valueHandler = DBUtils.findValueHandler(session, itemMeta);
        }
        try {
            try (DBCResultSet resultSet = JDBCResultSetImpl.makeResultSet(session, null, dbResult, ModelMessages.model_jdbc_array_result_set, true)) {
                List<Object> data = new ArrayList<>();
                while (dbResult.next()) {
                    // Fetch second column - it contains value
                    data.add(valueHandler.fetchValueObject(session, resultSet, type, 1));
                }
                return new JDBCCollection(type, valueHandler, data.toArray());
            }
        } finally {
            try {
                dbResult.close();
            } catch (SQLException e) {
                log.debug("Can't close array result set", e); //$NON-NLS-1$
            }
        }
    }

    @NotNull
    private static JDBCCollection extractDataFromArray(@NotNull JDBCSession session, @Nullable Array array, DBSDataType type, DBDValueHandler valueHandler) throws SQLException, DBCException {
        if (array == null) {
            return new JDBCCollection(type, valueHandler, null);
        }
        Object arrObject = array.getArray();
        int arrLength = arrObject == null ? 0 : java.lang.reflect.Array.getLength(arrObject);
        Object[] contents = new Object[arrLength];
        for (int i = 0; i < arrLength; i++) {
            Object item = java.lang.reflect.Array.get(arrObject, i);
            item = valueHandler.getValueFromObject(session, type, item, false);
            contents[i] = item;
        }
        return new JDBCCollection(type, valueHandler, contents);
    }

    public JDBCCollection(DBSDataType type, DBDValueHandler valueHandler, @Nullable Object[] contents) {
        this.type = type;
        this.valueHandler = valueHandler;
        this.contents = contents;
    }

    @NotNull
    @Override
    public DBSDataType getComponentType() {
        return type;
    }

    @NotNull
    @Override
    public DBDValueHandler getComponentValueHandler() {
        return valueHandler;
    }

    @Override
    public DBDValueCloneable cloneValue(DBRProgressMonitor monitor) {
        return new JDBCCollection(type, valueHandler, contents);
    }

    @Override
    public Object getRawValue() {
        return contents;
    }

    @Override
    public boolean isNull() {
        return contents == null;
    }

    @Override
    public void release() {
        contents = null;
    }

    public String toString() {
        if (isNull()) {
            return DBConstants.NULL_VALUE_LABEL;
        } else {
            return makeArrayString(DBDDisplayFormat.UI);
        }
    }

    @NotNull
    public String makeArrayString(DBDDisplayFormat format) {
        if (isNull()) {
            return SQLConstants.NULL_VALUE;
        }
        if (contents.length == 0) {
            return "";
        } else if (contents.length == 1) {
            return valueHandler.getValueDisplayString(type, contents[0], format);
        } else {
            StringBuilder str = new StringBuilder(contents.length * 32);
            for (Object item : contents) {
                if (str.length() > 0) {
                    str.append(' '); //$NON-NLS-1$
                }
                String itemString = valueHandler.getValueDisplayString(type, item, format);
                SQLUtils.appendValue(str, type, itemString);
            }
            return str.toString();
        }
    }

    @Override
    public int getItemCount() {
        return contents == null ? 0 : contents.length;
    }

    @Override
    public Object getItem(int index) {
        return contents[index];
    }

    @Override
    public void setItem(int index, Object value) {
        contents[index] = value;
    }

    @Override
    public void setContents(Object[] contents) {
        this.contents = contents;
    }

    public Array getArrayValue() throws DBCException {
        Object[] attrs = new Object[contents.length];
        for (int i = 0; i < contents.length; i++) {
            Object attr = contents[i];
            if (attr instanceof DBDValue) {
                attr = ((DBDValue) attr).getRawValue();
            }
            attrs[i] = attr;
        }
        final DBSDataType dataType = getComponentType();
        try (DBCSession session = DBUtils.openUtilSession(VoidProgressMonitor.INSTANCE, dataType.getDataSource(), "Create JDBC array")) {
            if (session instanceof Connection) {
                return ((Connection) session).createArrayOf(dataType.getTypeName(), attrs);
            } else {
                return new JDBCArrayImpl(dataType.getTypeName(), dataType.getTypeID(), attrs);
            }
        } catch (Throwable e) {
            throw new DBCException("Error creating struct", e);
        }
    }

}
