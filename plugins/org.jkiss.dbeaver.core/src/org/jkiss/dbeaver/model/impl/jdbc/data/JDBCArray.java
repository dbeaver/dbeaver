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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.data.DBDArray;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetImpl;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Array holder
 */
public class JDBCArray implements DBDArray, DBDValueCloneable {

    static final Log log = LogFactory.getLog(JDBCArray.class);

    private Object[] contents;
    private final DBSDataType type;
    private final DBDValueHandler valueHandler;

    @Nullable
    public static Object makeArray(JDBCSession session, Array array)
    {
        if (array == null) {
            return null;
        }
        DBSDataType type;
        if (session.getDataSource() instanceof DBPDataTypeProvider) {
            try {
                String baseTypeName = array.getBaseTypeName();
                type = ((DBPDataTypeProvider) session.getDataSource()).resolveDataType(session.getProgressMonitor(), baseTypeName);
                if (type == null) {
                    log.error("Can't resolve SQL array data type '" + baseTypeName + "'");
                    return null;
                }
            } catch (Exception e) {
                log.error("Error resolving data type", e);
                return null;
            }
        } else {
            return null;
        }
        DBDValueHandler valueHandler = DBUtils.findValueHandler(session, type);

        Object[] contents = null;
        try {
            try {
                contents = extractDataFromArray(session, array, type, valueHandler);
            } catch (SQLException e) {
                try {
                    contents = extractDataFromResultSet(session, array, type, valueHandler);
                } catch (SQLException e1) {
                    throw new DBCException(e1, session.getDataSource()); //$NON-NLS-1$
                }
            }
        } catch (DBCException e) {
            log.warn("Can't extract array data from JDBC array", e); //$NON-NLS-1$
        }
        return new JDBCArray(type, valueHandler, contents);
    }

    @Nullable
    private static Object[] extractDataFromResultSet(JDBCSession session, Array array, DBSDataType type, DBDValueHandler valueHandler) throws SQLException, DBCException
    {
        ResultSet dbResult = array.getResultSet();
        if (dbResult == null) {
            return null;
        }
        try {
            DBCResultSet resultSet = JDBCResultSetImpl.makeResultSet(session, dbResult, CoreMessages.model_jdbc_array_result_set);
            try {
                List<Object> data = new ArrayList<Object>();
                while (dbResult.next()) {
                    // Fetch second column - it contains value
                    data.add(valueHandler.fetchValueObject(session, resultSet, type, 1));
                }
                return data.toArray();
            } finally {
                resultSet.close();
            }
        }
        finally {
            try {
                dbResult.close();
            } catch (SQLException e) {
                log.debug("Could not close array result set", e); //$NON-NLS-1$
            }
        }
    }

    @Nullable
    private static Object[] extractDataFromArray(JDBCSession session, Array array, DBSDataType type, DBDValueHandler valueHandler) throws SQLException, DBCException
    {
        Object arrObject = array.getArray();
        if (arrObject == null) {
            return null;
        }
        int arrLength = java.lang.reflect.Array.getLength(arrObject);
        Object[] contents = new Object[arrLength];
        for (int i = 0; i < arrLength; i++) {
            Object item = java.lang.reflect.Array.get(arrObject, i);
            item = valueHandler.getValueFromObject(session, type, item, false);
            contents[i] = item;
        }
        return contents;
    }

    public JDBCArray(DBSDataType type, DBDValueHandler valueHandler, Object[] contents)
    {
        this.type = type;
        this.valueHandler = valueHandler;
        this.contents = contents;
    }

    @Override
    public DBSDataType getObjectDataType()
    {
        return type;
    }

    @Override
    public Object[] getContents() throws DBCException
    {
        return contents;
    }

    @Override
    public DBDValueCloneable cloneValue(DBRProgressMonitor monitor)
    {
        return new JDBCArray(type, valueHandler, contents);
    }

    @Override
    public boolean isNull()
    {
        return contents == null;
    }

    @Override
    public void release()
    {
        contents = null;
    }

    public String toString()
    {
        if (isNull()) {
            return DBConstants.NULL_VALUE_LABEL;
        } else {
            return makeArrayString();
        }
    }

    @Nullable
    public String makeArrayString()
    {
        if (isNull()) {
            return null;
        }
        if (contents.length == 0) {
            return "";
        } else if (contents.length == 1) {
            return valueHandler.getValueDisplayString(type, contents[0], DBDDisplayFormat.UI);
        } else {
            StringBuilder str = new StringBuilder(contents.length * 32);
            for (Object item : contents) {
                if (str.length() > 0) {
                    str.append(","); //$NON-NLS-1$
                }
                String itemString = valueHandler.getValueDisplayString(type, item, DBDDisplayFormat.UI);
                SQLUtils.appendValue(str, type, itemString);
            }
            return str.toString();
        }
    }

}
