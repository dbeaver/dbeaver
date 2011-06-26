/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.List;

/**
 * Oracle physical table
 */
public abstract class OracleTablePhysical extends OracleTableBase
{

    private boolean partitioned;
    private boolean valid;
    private long rowCount;
    private volatile String tablespaceName;
    private volatile OracleTablespace tablespace;
    private List<OracleIndex> indexes;

    public OracleTablePhysical(OracleSchema schema)
    {
        super(schema, false);
    }

    public OracleTablePhysical(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
        this.rowCount = JDBCUtils.safeGetLong(dbResult, "NUM_ROWS");
        this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
        this.partitioned = JDBCUtils.safeGetBoolean(dbResult, "PARTITIONED", "Y");
        this.tablespaceName = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");
    }

    @Property(name = "Row Count", viewable = true, order = 20)
    public long getRowCount()
    {
        return rowCount;
    }

    @Property(name = "Valid", viewable = true, order = 21)
    public boolean isValid()
    {
        return valid;
    }

    @Property(name = "Tablespace", viewable = true, order = 22)
    @LazyProperty(cacheValidator = TablespaceRetrieveValidator.class)
    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
    {
        final OracleDataSource dataSource = getDataSource();
        if (!dataSource.isAdmin()) {
            return tablespaceName;
        } else if (tablespace == null && !CommonUtils.isEmpty(tablespaceName)) {
            tablespace = dataSource.tablespaceCache.getObject(monitor, dataSource, tablespaceName);
            if (tablespace != null) {
                tablespaceName = null;
            } else {
                log.warn("Tablespace '" + tablespaceName + "' not found");
            }
        }
        return tablespace;
    }

    @Association
    public List<OracleIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        if (indexes == null) {
            // Read indexes using cache
            this.getContainer().indexCache.getObjects(monitor, getContainer(), this);
        }
        return indexes;
    }

    boolean isIndexesCached()
    {
        return indexes != null;
    }

    void setIndexes(List<OracleIndex> indexes)
    {
        this.indexes = indexes;
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        super.refreshEntity(monitor);

        indexes = null;
        return true;
    }

    public static class TablespaceRetrieveValidator implements IPropertyCacheValidator<OracleTablePhysical> {
        public boolean isPropertyCached(OracleTablePhysical object)
        {
            return
                object.tablespace instanceof OracleTablespace ||
                CommonUtils.isEmpty(object.tablespaceName) ||
                object.getDataSource().tablespaceCache.isCached() ||
                !object.getDataSource().isAdmin();
        }
    }

}
