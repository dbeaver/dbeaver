/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Logical foreign key
 */
public class ERDLogicalForeignKey implements DBSEntityAssociation, DBSEntityReferrer {

    private DBSEntity entity;
    private String name;
    private String description;
    private ERDLogicalPrimaryKey pk;
    private List<? extends DBSEntityAttributeRef> columns = new ArrayList<DBSEntityAttributeRef>();

    public ERDLogicalForeignKey(ERDEntity entity, String name, String description, ERDLogicalPrimaryKey pk)
    {
        this.entity = entity.getObject();
        this.name = name;
        this.description = description;
        this.pk = pk;
    }

    public DBSEntityConstraint getReferencedConstraint()
    {
        return pk;
    }

    public DBSEntity getAssociatedEntity()
    {
        return pk.getParentObject();
    }

    public DBPDataSource getDataSource()
    {
        return entity.getDataSource();
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public DBSEntity getParentObject()
    {
        return entity;
    }

    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return ERDConstants.CONSTRAINT_LOGICAL_FK;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isPersisted()
    {
        return false;
    }

    @Override
    public Collection<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }
}
