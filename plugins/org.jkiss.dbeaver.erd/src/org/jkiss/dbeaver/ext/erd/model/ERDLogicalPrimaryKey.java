/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.struct.AbstractConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Logical primary key
 */
public class ERDLogicalPrimaryKey extends AbstractConstraint<DBSTable> {

    private List<? extends DBSConstraintColumn> columns = new ArrayList<DBSConstraintColumn>();

    public ERDLogicalPrimaryKey(ERDTable table, String name, String description)
    {
        super(table.getObject(), name, description, DBSEntityConstraintType.PRIMARY_KEY);
    }

    public Collection<? extends DBSConstraintColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public String getFullQualifiedName()
    {
        return getName();
    }

    public DBPDataSource getDataSource()
    {
        return getTable().getDataSource();
    }
}
