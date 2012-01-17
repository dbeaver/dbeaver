/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Index cache implementation
 */
class PrimaryKeysCache extends JDBCCompositeCache<GenericStructContainer, GenericTable, GenericPrimaryKey, GenericConstraintColumn> {

    PrimaryKeysCache(TableCache tableCache)
    {
        super(tableCache, GenericTable.class, JDBCConstants.TABLE_NAME, JDBCConstants.PK_NAME);
    }

    protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, GenericStructContainer owner, GenericTable forParent)
        throws SQLException
    {
        try {
            return context.getMetaData().getPrimaryKeys(
                    owner.getCatalog() == null ? null : owner.getCatalog().getName(),
                    owner.getSchema() == null ? null : owner.getSchema().getName(),
                    forParent == null ? null : forParent.getName())
                .getSource();
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

    protected GenericPrimaryKey fetchObject(JDBCExecutionContext context, GenericStructContainer owner, GenericTable parent, String pkName, ResultSet dbResult)
        throws SQLException, DBException
    {
        return new GenericPrimaryKey(
            parent,
            pkName,
            null,
            DBSEntityConstraintType.PRIMARY_KEY,
            true);
    }

    protected GenericConstraintColumn fetchObjectRow(
        JDBCExecutionContext context,
        GenericTable parent, GenericPrimaryKey object, ResultSet dbResult)
        throws SQLException, DBException
    {
        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.COLUMN_NAME);
        if (CommonUtils.isEmpty(columnName)) {
            return null;
        }
        int keySeq = JDBCUtils.safeGetInt(dbResult, JDBCConstants.KEY_SEQ);

        GenericTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.warn("Column '" + columnName + "' not found in table '" + parent.getFullQualifiedName() + "' for PK '" + object.getFullQualifiedName() + "'");
            return null;
        }

        return new GenericConstraintColumn(object, tableColumn, keySeq);
    }

    protected Collection<GenericPrimaryKey> getObjectsCache(GenericTable parent)
    {
        return parent.getConstraintsCache();
    }

    protected void cacheObjects(GenericTable parent, List<GenericPrimaryKey> primaryKeys)
    {
        parent.setUniqueKeys(primaryKeys);
    }

    protected void cacheChildren(GenericPrimaryKey primaryKey, List<GenericConstraintColumn> rows)
    {
        primaryKey.setColumns(rows);
    }
}
