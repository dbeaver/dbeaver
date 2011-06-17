/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Index cache implementation
 */
class PrimaryKeysCache extends JDBCCompositeCache<GenericStructContainer, GenericTable, GenericPrimaryKey, GenericConstraintColumn> {

    PrimaryKeysCache(TableCache tableCache)
    {
        super(tableCache, GenericTable.class, JDBCConstants.TABLE_NAME, JDBCConstants.PK_NAME);
    }

    protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, GenericStructContainer owner, GenericTable forParent)
        throws SQLException, DBException
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
                throw new DBException("Global primary keys read not supported", e);
            } else {
                throw new DBException(e);
            }
        }
    }

    protected GenericPrimaryKey fetchObject(JDBCExecutionContext context, GenericTable parent, String pkName, ResultSet dbResult)
        throws SQLException, DBException
    {
        return new GenericPrimaryKey(
            parent,
            pkName,
            null,
            DBSConstraintType.PRIMARY_KEY,
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

    protected boolean isObjectsCached(GenericTable parent)
    {
        return parent.isConstraintsCached();
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
