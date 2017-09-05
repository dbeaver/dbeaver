/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
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
import org.jkiss.utils.CommonUtils;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Array holder
 */
public class JDBCCollection implements DBDCollection, DBDValueCloneable {

    private static final Log log = Log.getLog(JDBCCollection.class);

    private Object[] contents;
    private final DBSDataType type;
    private final DBDValueHandler valueHandler;
    private boolean modified;

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
    public boolean isModified() {
        return modified;
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
            str.append("[");
            for (int i = 0; i < contents.length; i++) {
                Object item = contents[i];
                if (i > 0) str.append(','); //$NON-NLS-1$
                String itemString = valueHandler.getValueDisplayString(type, item, format);
                SQLUtils.appendValue(str, type, itemString);
            }
            str.append("]");
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
        modified = true;
    }

    @Override
    public void setContents(Object[] contents) {
        this.contents = contents;
        this.modified = true;
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
        try (DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), dataType.getDataSource(), "Create JDBC array")) {
            if (session instanceof Connection) {
                return ((Connection) session).createArrayOf(dataType.getTypeName(), attrs);
            } else {
                return new JDBCArrayImpl(dataType.getTypeName(), dataType.getTypeID(), attrs);
            }
        } catch (Throwable e) {
            throw new DBCException("Error creating struct", e);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // Utilities
    /////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public static JDBCCollection makeCollectionFromArray(@NotNull JDBCSession session, @NotNull DBSTypedObject column, Array array) throws DBCException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        DBSDataType elementType = null;
        if (column instanceof DBSTypedObjectEx) {
            DBSDataType arrayType = ((DBSTypedObjectEx) column).getDataType();
            if (arrayType != null) {
                try {
                    elementType = arrayType.getComponentType(monitor);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }
        }
        if (elementType == null) {
            try {
                if (array == null) {
                    String arrayTypeName = column.getTypeName();
                    DBSDataType arrayType = session.getDataSource().resolveDataType(monitor, arrayTypeName);
                    if (arrayType != null) {
                        elementType = arrayType.getComponentType(monitor);
                    }
                } else {
                    String baseTypeName = array.getBaseTypeName();
                    elementType = session.getDataSource().resolveDataType(monitor, baseTypeName);
                }
            } catch (Exception e) {
                throw new DBCException("Error resolving data type", e);
            }
        }

        try {
            if (elementType == null) {
                if (array == null) {
                    throw new DBCException("Can't resolve NULL array data type");
                }
                try {
                    return makeCollectionFromResultSet(session, array, null);
                } catch (SQLException e) {
                    throw new DBCException(e, session.getDataSource()); //$NON-NLS-1$
                }
            }
            try {
                return makeCollectionFromArray(session, array, elementType);
            } catch (SQLException e) {
                if (array == null) {
                    throw new DBCException(e, session.getDataSource()); //$NON-NLS-1$
                }
                try {
                    return makeCollectionFromResultSet(session, array, elementType);
                } catch (SQLException e1) {
                    throw new DBCException(e1, session.getDataSource()); //$NON-NLS-1$
                }
            }
        } catch (DBException e) {
            throw new DBCException("Can't extract array data from JDBC array", e); //$NON-NLS-1$
        }
    }

    @NotNull
    private static JDBCCollection makeCollectionFromResultSet(@NotNull JDBCSession session, @NotNull Array array, @Nullable DBSDataType elementType) throws SQLException, DBException {
        ResultSet dbResult = array.getResultSet();
        if (dbResult == null) {
            throw new DBCException("JDBC array type was not resolved and result set was not provided by driver. Return NULL.");
        }
        DBDValueHandler valueHandler;
        if (elementType == null) {
            JDBCColumnMetaData itemMeta = new JDBCColumnMetaData(session.getDataSource(), dbResult.getMetaData(), 1);
            elementType = DBUtils.resolveDataType(session.getProgressMonitor(), session.getDataSource(), itemMeta.getTypeName());
            if (elementType == null) {
                elementType = new JDBCDataType(session.getDataSource(), itemMeta);
            }
            valueHandler = DBUtils.findValueHandler(session, itemMeta);
        } else {
            valueHandler = DBUtils.findValueHandler(session, elementType);
        }
        try {
            try (DBCResultSet resultSet = JDBCResultSetImpl.makeResultSet(session, null, dbResult, ModelMessages.model_jdbc_array_result_set, true)) {
                List<Object> data = new ArrayList<>();
                while (dbResult.next()) {
                    // Fetch second column - it contains value
                    data.add(valueHandler.fetchValueObject(session, resultSet, elementType, 1));
                }
                return new JDBCCollection(elementType, valueHandler, data.toArray());
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
    private static JDBCCollection makeCollectionFromArray(@NotNull JDBCSession session, @Nullable Array array, @NotNull DBSDataType elementType) throws SQLException, DBCException {
        final DBDValueHandler elementValueHandler = DBUtils.findValueHandler(session, elementType);
        if (array == null) {
            return new JDBCCollection(elementType, elementValueHandler, null);
        }
        Object arrObject = array.getArray();
        return makeCollectionFromJavaArray(session, elementType, elementValueHandler, arrObject);
    }

    @NotNull
    private static JDBCCollection makeCollectionFromJavaArray(
            @NotNull JDBCSession session,
            @NotNull DBSDataType elementType,
            @NotNull DBDValueHandler elementValueHandler,
            @Nullable Object arrObject)
            throws DBCException
    {
        int arrLength = arrObject == null ? 0 : java.lang.reflect.Array.getLength(arrObject);
        Object[] contents = new Object[arrLength];
        Object itemValue;
        for (int i = 0; i < arrLength; i++) {
            Object item = java.lang.reflect.Array.get(arrObject, i);
            if (item != null && item.getClass().isArray() && elementType.getDataKind() != DBPDataKind.ARRAY) {
                // This may happen in case of multidimensional array
                itemValue = makeCollectionFromJavaArray(session, elementType, elementValueHandler, item);
            } else {
                itemValue = elementValueHandler.getValueFromObject(session, elementType, item, false);
            }
            contents[i] = itemValue;
        }
        return new JDBCCollection(elementType, elementValueHandler, contents);
    }

    @NotNull
    public static DBDCollection makeCollectionFromString(@NotNull JDBCSession session, String value) throws DBCException {
        String stringType = DBUtils.getDefaultDataTypeName(session.getDataSource(), DBPDataKind.STRING);
        if (stringType == null) {
            throw new DBCException("String data type not supported by database");
        }
        DBSDataType dataType = DBUtils.getLocalDataType(session.getDataSource(), stringType);
        if (dataType == null) {
            throw new DBCException("String data type '" + stringType + "' not supported by database");
        }
        DBDValueHandler valueHandler = DBUtils.findValueHandler(session, dataType);

        // Try to divide on string elements
        if (!CommonUtils.isEmpty(value)) {
            if (value.startsWith("[") && value.endsWith("]")) {
                // FIXME: use real parser (nested arrays, quotes escape, etc)
                String arrayString = value.substring(1, value.length() - 1);
                List<Object> items = new ArrayList<>();
                StringTokenizer st = new StringTokenizer(arrayString, ",", false);
                while (st.hasMoreTokens()) {
                    String token = st.nextToken().trim();
                    if (token.startsWith("\"") && token.endsWith("\"")) {
                        token = token.substring(1, token.length() - 1);
                    }
                    items.add(token);
                }

                return new JDBCCollectionString(dataType, valueHandler, value, items.toArray() );
            }
        }
        return new JDBCCollectionString(dataType, valueHandler, value);
    }

}
