/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.struct.AbstractForeignKey;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Logical foreign key
 */
public class ERDLogicalForeignKey extends AbstractForeignKey<DBPDataSource, DBSTable, ERDLogicalPrimaryKey> {

    private List<? extends DBSForeignKeyColumn> columns = new ArrayList<DBSForeignKeyColumn>();

    public ERDLogicalForeignKey(ERDTable table, String name, String description, ERDLogicalPrimaryKey pk)
    {
        super(table.getObject(), name, description, pk, DBSConstraintCascade.NO_ACTION, DBSConstraintCascade.NO_ACTION);
    }

    public DBSConstraintType getConstraintType()
    {
        return ERDConstants.CONSTRAINT_LOGICAL_FK;
    }

    public DBSConstraintDefferability getDefferability()
    {
        return DBSConstraintDefferability.UNKNOWN;
    }

    public Collection<? extends DBSForeignKeyColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public String getFullQualifiedName()
    {
        return getName();
    }
}
