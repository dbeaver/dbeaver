/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTableConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Logical primary key
 */
public class ERDLogicalPrimaryKey extends AbstractTableConstraint<DBSTable> {

    private List<? extends DBSTableConstraintColumn> columns = new ArrayList<DBSTableConstraintColumn>();

    public ERDLogicalPrimaryKey(ERDEntity entity, String name, String description)
    {
        super(entity.getObject(), name, description, DBSEntityConstraintType.PRIMARY_KEY);
    }

    public Collection<? extends DBSTableConstraintColumn> getColumns(DBRProgressMonitor monitor)
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
