/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTableConstraint;
import org.jkiss.wmi.service.WMIObjectAttribute;

/**
 * Class association
 */
public class WMIClassReference extends WMIClassAttribute implements DBSEntityAssociation
{
    private WMIClass refClass;

    protected WMIClassReference(WMIClass wmiClass, WMIObjectAttribute attribute, WMIClass refClass)
    {
        super(wmiClass, attribute);
        this.refClass = refClass;
    }

    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.ASSOCIATION;
    }

    @Override
    public DBSEntity getAssociatedEntity()
    {
        return refClass;
    }

    @Override
    public DBSTableConstraint getReferencedConstraint()
    {
        return null;
    }

}
