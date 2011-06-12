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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Index cache implementation
 */
class PrimaryKeysCache extends JDBCCompositeCache<GenericTable, GenericPrimaryKey, GenericConstraintColumn> {
    private GenericStructContainer structContainer;

    PrimaryKeysCache(GenericStructContainer structContainer)
    {
        super(structContainer.getTableCache(), GenericTable.class, JDBCConstants.TABLE_NAME, JDBCConstants.PK_NAME);
        this.structContainer = structContainer;
    }

    protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, GenericTable forParent)
        throws SQLException, DBException
    {
        try {
            return context.getMetaData().getPrimaryKeys(
                    structContainer.getCatalog() == null ? null : structContainer.getCatalog().getName(),
                    structContainer.getSchema() == null ? null : structContainer.getSchema().getName(),
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

    protected GenericPrimaryKey fetchObject(JDBCExecutionContext context, ResultSet dbResult, GenericTable parent, String pkName)
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
        ResultSet dbResult,
        GenericTable parent,
        GenericPrimaryKey object)
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

    protected void cacheObjects(DBRProgressMonitor monitor, GenericTable parent, List<GenericPrimaryKey> primaryKeys)
    {
        parent.setUniqueKeys(primaryKeys);
    }

    protected void cacheRows(DBRProgressMonitor monitor, GenericPrimaryKey primaryKey, List<GenericConstraintColumn> rows)
    {
        primaryKey.setColumns(rows);
    }
}
