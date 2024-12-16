/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.AbstractDatabaseList;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCArrayImpl;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetImpl;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Array holder
 */
public class JDBCCollection extends AbstractDatabaseList implements DBDValueCloneable {

    private static final Log log = Log.getLog(JDBCCollection.class);

    private Object[] contents;
    private DBSDataType type;
    private DBDValueHandler valueHandler;
    private boolean modified;

    public JDBCCollection() {
    }

    public JDBCCollection(@NotNull DBRProgressMonitor monitor, DBSDataType type, DBDValueHandler valueHandler, @Nullable Object[] contents) {
        this.type = type;
        this.valueHandler = valueHandler;
        if (contents != null) {
            this.contents = new Object[contents.length];
            for (int i = 0; i < contents.length; i++) {
                Object value = contents[i];
                if (value instanceof DBDValueCloneable) {
                    try {
                        value = ((DBDValueCloneable) value).cloneValue(monitor);
                    } catch (DBCException ex) {
                        log.warn("Failed to clone value of type " + value.getClass().getName(), ex);
                    }
                }
                this.contents[i] = value;
            }
        } else {
            this.contents = null;
        }
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
        return new JDBCCollection(monitor, type, valueHandler, contents);
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
            return makeArrayString('{', '}');
        }
    }

    @NotNull
    public String makeArrayString(char ... brackets) {
        if (isNull()) {
            return SQLConstants.NULL_VALUE;
        }
        // This probably was used for pretty-printing, we can't be sure now.
        // In any way, this makes interpreting empty and one-length arrays
        // hard by eye, and some databases complain about malformed format.
        //
        // if (contents.length == 0) {
        //     return "";
        // } else if (contents.length == 1) {
        //     return valueHandler.getValueDisplayString(type, contents[0], format);
        // } else {
        StringBuilder str = new StringBuilder(contents.length * 32);
        str.append(brackets[0]);
        for (int i = 0; i < contents.length; i++) {
            Object item = contents[i];
            if (i > 0) str.append(','); //$NON-NLS-1$
            String itemString = valueHandler.getValueDisplayString(type, item, DBDDisplayFormat.NATIVE);
            SQLUtils.appendValue(str, type, itemString);
        }
        // }
        str.append(brackets[1]);
        return str.toString();
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
        if (contents == null) {
            return null;
        }
        Object[] attrs = new Object[contents.length];
        for (int i = 0; i < contents.length; i++) {
            Object attr = contents[i];
            if (attr instanceof DBDValue) {
                attr = ((DBDValue) attr).getRawValue();
            }
            attrs[i] = attr;
        }
        final DBSDataType dataType = getComponentType();
        try (DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), dataType, "Create JDBC array")) {
            if (session instanceof Connection) {
                String typeName = dataType.getFullTypeName();
                return ((Connection) session).createArrayOf(typeName, attrs);
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

        DBSDataType arrayType = null, elementType = null;
        JDBCDataSource dataSource = session.getDataSource();
        try {
            if (column instanceof DBSTypedObjectEx) {
                arrayType = ((DBSTypedObjectEx) column).getDataType();
            } else {
                if (column instanceof DBCAttributeMetaData) {
                    DBCEntityMetaData entityMetaData = ((DBCAttributeMetaData) column).getEntityMetaData();
                    if (entityMetaData != null) {
                        DBSEntity docEntity = DBUtils.getEntityFromMetaData(session.getProgressMonitor(), session.getExecutionContext(), entityMetaData);
                        if (docEntity != null) {
                            DBSEntityAttribute attribute = docEntity.getAttribute(session.getProgressMonitor(), ((DBCAttributeMetaData) column).getName());
                            if (attribute instanceof DBSTypedObjectEx) {
                                arrayType = ((DBSTypedObjectEx) attribute).getDataType();
                            }
                        }
                    }
                }
                if (arrayType == null) {
                    arrayType = dataSource.resolveDataType(session.getProgressMonitor(), column.getFullTypeName());
                }
            }
            if (arrayType != null) {
                elementType = arrayType.getComponentType(monitor);
            }
        } catch (DBException e) {
            log.debug("Error getting array component type", e);
        }

        if (elementType == null) {
            try {
                if (array != null) {
                    String baseTypeName = array.getBaseTypeName();
                    if (baseTypeName != null) {
                        // Strip type name [Presto, #6025]
                        baseTypeName = SQLUtils.stripColumnTypeModifiers(baseTypeName);
                        elementType = dataSource.resolveDataType(monitor, baseTypeName);
                    }
                }
            } catch (Exception e) {
                throw new DBCException("Error resolving data type", e);
            }
        }

        try {
            if (elementType == null && array == null) {
                // Null array of unknown type. Just make NULL read-only array
                String defDataTypeName = dataSource.getDefaultDataTypeName(DBPDataKind.OBJECT);
                DBSDataType defDataType = dataSource.getLocalDataType(defDataTypeName);
                DBDValueHandler defValueHandler = session.getDefaultValueHandler();
                return new JDBCCollection(monitor, defDataType, defValueHandler, null);
            }

            if (elementType != null) {
                try {
                    return makeCollectionFromArray(session, array, elementType);
                } catch (SQLException e) {
                    if (array == null) {
                        throw new DBCException(e, session.getExecutionContext());
                    }
                }
            }

            try {
                return makeCollectionFromResultSet(session, array, null);
            } catch (SQLException e) {
                try {
                    // Array of unknown type.
                    return makeCollectionFromJavaArray(session, array.getArray());
                } catch (SQLException ex) {
                    throw new DBCException(ex, session.getExecutionContext());
                }
            }
        } catch (DBException e) {
            throw new DBCException("Can't extract array data from JDBC array", e, session.getExecutionContext()); //$NON-NLS-1$
        }
    }

    @NotNull
    public static JDBCCollection makeCollectionFromJavaArray(@NotNull JDBCSession session, Object array) throws DBCException {
        DBSDataType elementType;
        DBPDataKind dataKind;
        DBPDataTypeProvider dataTypeProvider = session.getDataSource();
        if (array instanceof int[]) {
            dataKind = DBPDataKind.NUMERIC;
            elementType = dataTypeProvider.getLocalDataType(Types.INTEGER);
        } else if (array instanceof short[]) {
            dataKind = DBPDataKind.NUMERIC;
            elementType = dataTypeProvider.getLocalDataType(Types.SMALLINT);
        } else if (array instanceof byte[]) {
            dataKind = DBPDataKind.NUMERIC;
            elementType = dataTypeProvider.getLocalDataType(Types.BINARY);
        } else if (array instanceof long[]) {
            dataKind = DBPDataKind.NUMERIC;
            elementType = dataTypeProvider.getLocalDataType(Types.BIGINT);
        } else if (array instanceof float[]) {
            dataKind = DBPDataKind.NUMERIC;
            elementType = dataTypeProvider.getLocalDataType(Types.FLOAT);
            if (elementType == null) {
                elementType = dataTypeProvider.getLocalDataType(Types.DOUBLE);
            }
        } else if (array instanceof double[]) {
            dataKind = DBPDataKind.NUMERIC;
            elementType = dataTypeProvider.getLocalDataType(Types.DOUBLE);
            if (elementType == null) {
                elementType = dataTypeProvider.getLocalDataType(Types.FLOAT);
            }
        } else if (array instanceof BigDecimal[]) {
            dataKind = DBPDataKind.NUMERIC;
            elementType = dataTypeProvider.getLocalDataType(Types.DECIMAL);
        } else if (array instanceof boolean[]) {
            dataKind = DBPDataKind.BOOLEAN;
            elementType = dataTypeProvider.getLocalDataType(Types.BOOLEAN);
        } else if (array instanceof String[]) {
            dataKind = DBPDataKind.STRING;
            elementType = dataTypeProvider.getLocalDataType(Types.VARCHAR);
        } else if (array instanceof Date[]) {
            dataKind = DBPDataKind.DATETIME;
            elementType = dataTypeProvider.getLocalDataType(Types.TIMESTAMP);
        } else {
            dataKind = DBPDataKind.OBJECT;
            elementType = dataTypeProvider.getLocalDataType(Types.STRUCT);
        }
        if (elementType == null) {
            try {
                String typeName = dataTypeProvider.getDefaultDataTypeName(dataKind);
                if (typeName != null) {
                    elementType = dataTypeProvider.getLocalDataType(typeName);
                }
            } catch (Exception e) {
                throw new DBCException("Error resolving default data type", e);
            }
        }

        try {
            if (elementType == null) {
                throw new DBCException("Can't resolve array element data type"); //$NON-NLS-1$
            }
            final DBDValueHandler elementValueHandler = DBUtils.findValueHandler(session, elementType);
            if (array == null) {
                return new JDBCCollection(session.getProgressMonitor(), elementType, elementValueHandler, null);
            }
            return makeCollectionFromJavaArray(session, elementType, elementValueHandler, array);
        } catch (DBException e) {
            throw new DBCException("Can't extract array data from Java array", e); //$NON-NLS-1$
        }
    }

    @NotNull
    public static JDBCCollection makeCollectionFromJavaCollection(@NotNull JDBCSession session, @NotNull DBSTypedObject column, Collection<?> coll) throws DBCException {
        DBPDataTypeProvider dataTypeProvider = session.getDataSource();
        DBPDataKind dataKind = DBPDataKind.OBJECT;
        DBSDataType elementType = dataTypeProvider.getLocalDataType(Types.STRUCT);
        if (elementType == null) {
            try {
                String typeName = dataTypeProvider.getDefaultDataTypeName(dataKind);
                if (typeName != null) {
                    elementType = dataTypeProvider.getLocalDataType(typeName);
                }
            } catch (Exception e) {
                throw new DBCException("Error resolving default data type", e);
            }
        }

        try {
            if (elementType == null) {
                throw new DBCException("Can't resolve array element data type"); //$NON-NLS-1$
            }
            final DBDValueHandler elementValueHandler = DBUtils.findValueHandler(session, elementType);
            return makeCollectionFromJavaArray(session, elementType, elementValueHandler, coll.toArray());
        } catch (DBException e) {
            throw new DBCException("Can't extract array data from Java array", e); //$NON-NLS-1$
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
                return new JDBCCollection(session.getProgressMonitor(), elementType, valueHandler, data.toArray());
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
            return new JDBCCollection(session.getProgressMonitor(), elementType, elementValueHandler, null);
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
                itemValue = elementValueHandler.getValueFromObject(session, elementType, item, false, true);
            }
            contents[i] = itemValue;
        }
        return new JDBCCollection(session.getProgressMonitor(), elementType, elementValueHandler, contents);
    }

    @NotNull
    public static DBDCollection makeCollectionFromString(@NotNull JDBCSession session, String value) throws DBCException {
        String stringType = DBUtils.getDefaultDataTypeName(session.getDataSource(), DBPDataKind.STRING);
        DBSDataType dataType = DBUtils.getLocalDataType(session.getDataSource(), stringType);
        DBDValueHandler valueHandler;
        if (dataType == null) {
            log.debug("String data type '" + stringType + "' not supported by database");
            valueHandler = session.getDataSource().getContainer().getDefaultValueHandler();
        } else {
            valueHandler = DBUtils.findValueHandler(session, dataType);
        }

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

                return new JDBCCollectionString(session.getProgressMonitor(), dataType, valueHandler, value, items.toArray());
            }
        }
        return new JDBCCollectionString(session.getProgressMonitor(), dataType, valueHandler, value);
    }

    //////////////////////////////////////////
    // List implementation

    @Override
    public int size() {
        return getItemCount();
    }

    @Override
    public Object get(int index) {
        return getItem(index);
    }

}
