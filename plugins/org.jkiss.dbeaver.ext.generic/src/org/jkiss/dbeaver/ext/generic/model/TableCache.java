/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

/**
 * Generic tables cache implementation
 */
public class TableCache extends JDBCStructLookupCache<GenericStructContainer, GenericTableBase, GenericTableColumn> {

    private static final Log log = Log.getLog(TableCache.class);

    final GenericDataSource dataSource;
    final GenericMetaObject tableObject;
    final GenericMetaObject columnObject;

    TableCache(GenericDataSource dataSource)
    {
        super(GenericUtils.getColumn(dataSource, GenericConstants.OBJECT_TABLE, JDBCConstants.TABLE_NAME));
        this.dataSource = dataSource;
        this.tableObject = dataSource.getMetaObject(GenericConstants.OBJECT_TABLE);
        this.columnObject = dataSource.getMetaObject(GenericConstants.OBJECT_TABLE_COLUMN);
        setListOrderComparator(DBUtils.<GenericTableBase>nameComparator());
    }

    public GenericDataSource getDataSource()
    {
        return dataSource;
    }

    @NotNull
    @Override
    public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase object, @Nullable String objectName) throws SQLException {
        return dataSource.getMetaModel().prepareTableLoadStatement(session, owner, object, objectName);
    }

    @Nullable
    @Override
    protected GenericTableBase fetchObject(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        return getDataSource().getMetaModel().createTableImpl(session, owner, tableObject, dbResult);
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forTable)
        throws SQLException
    {
        return dataSource.getMetaModel().prepareTableColumnLoadStatement(session, owner, forTable);
    }

    @Override
    protected GenericTableColumn fetchChild(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @NotNull GenericTableBase table, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        String columnName = GenericUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.COLUMN_NAME);
        int valueType = GenericUtils.safeGetInt(columnObject, dbResult, JDBCConstants.DATA_TYPE);
        int sourceType = GenericUtils.safeGetInt(columnObject, dbResult, JDBCConstants.SOURCE_DATA_TYPE);
        String typeName = GenericUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.TYPE_NAME);
        long columnSize = GenericUtils.safeGetLong(columnObject, dbResult, JDBCConstants.COLUMN_SIZE);
        boolean isNotNull = GenericUtils.safeGetInt(columnObject, dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.columnNoNulls;
        Integer scale = null;
        try {
            scale = GenericUtils.safeGetInteger(columnObject, dbResult, JDBCConstants.DECIMAL_DIGITS);
        } catch (Throwable e) {
            log.warn("Error getting column scale", e);
        }
        Integer precision = null;
        if (valueType == Types.NUMERIC || valueType == Types.DECIMAL) {
            precision = (int) columnSize;
        }
        int radix = 10;
        try {
            radix = GenericUtils.safeGetInt(columnObject, dbResult, JDBCConstants.NUM_PREC_RADIX);
        } catch (Exception e) {
            log.warn("Error getting column radix", e);
        }
        String defaultValue = GenericUtils.safeGetString(columnObject, dbResult, JDBCConstants.COLUMN_DEF);
        String remarks = GenericUtils.safeGetString(columnObject, dbResult, JDBCConstants.REMARKS);
        long charLength = GenericUtils.safeGetLong(columnObject, dbResult, JDBCConstants.CHAR_OCTET_LENGTH);
        int ordinalPos = GenericUtils.safeGetInt(columnObject, dbResult, JDBCConstants.ORDINAL_POSITION);
        boolean autoIncrement = "YES".equals(GenericUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.IS_AUTOINCREMENT));
        boolean autoGenerated = "YES".equals(GenericUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.IS_GENERATEDCOLUMN));
        if (!CommonUtils.isEmpty(typeName)) {
            // Check for identity modifier [DBSPEC: MS SQL]
            if (typeName.toUpperCase(Locale.ENGLISH).endsWith(GenericConstants.TYPE_MODIFIER_IDENTITY)) {
                autoIncrement = true;
                typeName = typeName.substring(0, typeName.length() - GenericConstants.TYPE_MODIFIER_IDENTITY.length());
            }
            // Check for empty modifiers [MS SQL]
            if (typeName.endsWith("()")) {
                typeName = typeName.substring(0, typeName.length() - 2);
            }
        } else {
            typeName = "N/A";
        }

        {
            // Fix value type
            DBSDataType dataType = dataSource.getLocalDataType(typeName);
            if (dataType != null) {
                valueType = dataType.getTypeID();
            }
        }

        return getDataSource().getMetaModel().createTableColumnImpl(
            session.getProgressMonitor(),
            dbResult,
            table,
            columnName,
            typeName, valueType, sourceType, ordinalPos,
            columnSize,
            charLength, scale, precision, radix, isNotNull,
            remarks, defaultValue, autoIncrement, autoGenerated
        );
    }

}
