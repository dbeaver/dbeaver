/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.List;

/**
 * Oracle physical table
 */
public abstract class OracleTablePhysical extends OracleTableBase
{

    private boolean valid;
    private long rowCount;
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

}
