/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;

import java.util.List;

/**
 * GenericProcedure
 */
public class GenericProcedure extends AbstractProcedure<GenericDataSource, GenericEntityContainer>
{
    static final Log log = LogFactory.getLog(GenericProcedure.class);

    private DBSProcedureType procedureType;
    private List<GenericProcedureColumn> columns;

    public GenericProcedure(
        GenericEntityContainer container,
        String procedureName,
        String description,
        DBSProcedureType procedureType)
    {
        super(container, procedureName, description);
        this.procedureType = procedureType;
    }

    public DBSProcedureType getProcedureType()
    {
        return procedureType ;
    }

    @Property(name = "Catalog", viewable = true, order = 3)
    public GenericCatalog getCatalog()
    {
        return getContainer().getCatalog();
    }

    @Property(name = "Schema", viewable = true, order = 4)
    public GenericSchema getSchema()
    {
        return getContainer().getSchema();
    }

    public List<GenericProcedureColumn> getColumns(DBRProgressMonitor monitor)
        throws DBException
    {
        if (columns == null) {
            getContainer().getProcedureCache().loadChildren(monitor, this);
        }
        return columns;
    }

    boolean isColumnsCached()
    {
        return columns != null;
    }

    public void cacheColumns(List<GenericProcedureColumn> columns)
    {
        this.columns = columns;
    }

}
