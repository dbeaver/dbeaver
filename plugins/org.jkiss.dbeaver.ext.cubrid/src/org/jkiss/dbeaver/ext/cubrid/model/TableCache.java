/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

/**
 * Cubrid tables cache implementation
 */
public class TableCache extends JDBCStructLookupCache<CubridStructContainer, CubridTableBase, CubridTableColumn> {

    private static final Log log = Log.getLog(TableCache.class);

    final CubridDataSource dataSource;
    final CubridMetaObject tableObject;
    final CubridMetaObject columnObject;

    protected TableCache(CubridDataSource dataSource)
    {
        super(CubridUtils.getColumn(dataSource, CubridConstants.OBJECT_TABLE, JDBCConstants.TABLE_NAME));
        this.dataSource = dataSource;
        this.tableObject = dataSource.getMetaObject(CubridConstants.OBJECT_TABLE);
        this.columnObject = dataSource.getMetaObject(CubridConstants.OBJECT_TABLE_COLUMN);
        setListOrderComparator(DBUtils.<CubridTableBase>nameComparatorIgnoreCase());
    }

    public CubridDataSource getDataSource()
    {
        return dataSource;
    }

    @NotNull
    @Override
    public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer owner, @Nullable CubridTableBase object, @Nullable String objectName) throws SQLException {
        return dataSource.getMetaModel().prepareTableLoadStatement(session, owner, object, objectName);
    }

    @Nullable
    @Override
    protected CubridTableBase fetchObject(@NotNull JDBCSession session, @NotNull CubridStructContainer owner, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        return getDataSource().getMetaModel().createTableImpl(session, owner, tableObject, dbResult);
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer owner, @Nullable CubridTableBase forTable)
        throws SQLException
    {
        return dataSource.getMetaModel().prepareTableColumnLoadStatement(session, owner, forTable);
    }

    @Override
    protected CubridTableColumn fetchChild(@NotNull JDBCSession session, @NotNull CubridStructContainer owner, @NotNull CubridTableBase table, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        boolean trimName = dataSource.getMetaModel().isTrimObjectNames();
        String columnName = trimName ?
            CubridUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.COLUMN_NAME)
            : CubridUtils.safeGetString(columnObject, dbResult, JDBCConstants.COLUMN_NAME);
        int valueType = CubridUtils.safeGetInt(columnObject, dbResult, JDBCConstants.DATA_TYPE);
        int sourceType = CubridUtils.safeGetInt(columnObject, dbResult, JDBCConstants.SOURCE_DATA_TYPE);
        String typeName = CubridUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.TYPE_NAME);
        long columnSize = CubridUtils.safeGetLong(columnObject, dbResult, JDBCConstants.COLUMN_SIZE);
        boolean isNotNull = CubridUtils.safeGetInt(columnObject, dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.columnNoNulls;
        Integer scale = null;
        try {
            scale = CubridUtils.safeGetInteger(columnObject, dbResult, JDBCConstants.DECIMAL_DIGITS);
        } catch (Throwable e) {
            log.warn("Error getting column scale", e);
        }
        Integer precision = null;
        if (valueType == Types.NUMERIC || valueType == Types.DECIMAL) {
            precision = (int) columnSize;
        }
        int radix = 10;
        try {
            radix = CubridUtils.safeGetInt(columnObject, dbResult, JDBCConstants.NUM_PREC_RADIX);
        } catch (Exception e) {
            log.warn("Error getting column radix", e);
        }
        String defaultValue = CubridUtils.safeGetString(columnObject, dbResult, JDBCConstants.COLUMN_DEF);
        String remarks = CubridUtils.safeGetString(columnObject, dbResult, JDBCConstants.REMARKS);
        long charLength = CubridUtils.safeGetLong(columnObject, dbResult, JDBCConstants.CHAR_OCTET_LENGTH);
        int ordinalPos = CubridUtils.safeGetInt(columnObject, dbResult, JDBCConstants.ORDINAL_POSITION);
        boolean autoIncrement = "YES".equals(CubridUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.IS_AUTOINCREMENT));
        boolean autoGenerated = "YES".equals(CubridUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.IS_GENERATEDCOLUMN));
        if (!CommonUtils.isEmpty(typeName)) {
            // Check for identity modifier [DBSPEC: MS SQL]
            if (typeName.toUpperCase(Locale.ENGLISH).endsWith(CubridConstants.TYPE_MODIFIER_IDENTITY)) {
                autoIncrement = true;
                typeName = typeName.substring(0, typeName.length() - CubridConstants.TYPE_MODIFIER_IDENTITY.length());
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

    @Override
    public void beforeCacheLoading(JDBCSession session, CubridStructContainer owner) throws DBException {
       // Do nothing
    }

    @Override
    public void afterCacheLoading(JDBCSession session, CubridStructContainer owner) {
        // Do nothing
    }
}
