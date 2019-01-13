/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Index cache implementation
 */
class PrimaryKeysCache extends JDBCCompositeCache<GenericStructContainer, GenericTable, GenericPrimaryKey, GenericTableConstraintColumn> {

    private final GenericMetaObject pkObject;

    PrimaryKeysCache(TableCache tableCache)
    {
        super(
            tableCache,
            GenericTable.class,
            GenericUtils.getColumn(tableCache.getDataSource(), GenericConstants.OBJECT_PRIMARY_KEY, JDBCConstants.TABLE_NAME),
            GenericUtils.getColumn(tableCache.getDataSource(), GenericConstants.OBJECT_PRIMARY_KEY, JDBCConstants.PK_NAME));
        pkObject = tableCache.getDataSource().getMetaObject(GenericConstants.OBJECT_PRIMARY_KEY);
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, GenericStructContainer owner, GenericTable forParent)
        throws SQLException
    {
        try {
            return session.getMetaData().getPrimaryKeys(
                    owner.getCatalog() == null ? null : owner.getCatalog().getName(),
                    owner.getSchema() == null ? null : owner.getSchema().getName(),
                    forParent == null ? owner.getDataSource().getAllObjectsPattern() : forParent.getName())
                .getSourceStatement();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            if (forParent == null) {
                throw new SQLException("Global primary keys read not supported", e);
            } else {
                throw new SQLException(e);
            }
        }
    }

    protected String getDefaultObjectName(JDBCResultSet dbResult, String parentName) {
        int keySeq = GenericUtils.safeGetInt(pkObject, dbResult, JDBCConstants.KEY_SEQ);
        return parentName.toUpperCase(Locale.ENGLISH) + "_PK";
    }

    @Nullable
    @Override
    protected GenericPrimaryKey fetchObject(JDBCSession session, GenericStructContainer owner, GenericTable parent, String pkName, JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        return new GenericPrimaryKey(
            parent,
            pkName,
            null,
            DBSEntityConstraintType.PRIMARY_KEY,
            true);
    }

    @Nullable
    @Override
    protected GenericTableConstraintColumn[] fetchObjectRow(
        JDBCSession session,
        GenericTable parent, GenericPrimaryKey object, JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        String columnName = GenericUtils.safeGetStringTrimmed(pkObject, dbResult, JDBCConstants.COLUMN_NAME);
        if (CommonUtils.isEmpty(columnName)) {
            log.debug("Null primary key column for '" + object.getName() + "'");
            return null;
        }
        if ((columnName.startsWith("[") && columnName.endsWith("]")) ||
            (columnName.startsWith(SQLConstants.DEFAULT_IDENTIFIER_QUOTE) && columnName.endsWith(SQLConstants.DEFAULT_IDENTIFIER_QUOTE))) {
            // [JDBC: SQLite] Escaped column name. Let's un-escape it
            columnName = columnName.substring(1, columnName.length() - 1);
        }
        int keySeq = GenericUtils.safeGetInt(pkObject, dbResult, JDBCConstants.KEY_SEQ);

        GenericTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.warn("Column '" + columnName + "' not found in table '" + parent.getFullyQualifiedName(DBPEvaluationContext.DDL) + "' for PK '" + object.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
            return null;
        }

        return new GenericTableConstraintColumn[] {
            new GenericTableConstraintColumn(object, tableColumn, keySeq) };
    }

    @Override
    protected void cacheChildren(DBRProgressMonitor monitor, GenericPrimaryKey primaryKey, List<GenericTableConstraintColumn> rows)
    {
        primaryKey.setColumns(rows);
    }

}
