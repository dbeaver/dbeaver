package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.Collection;
import java.util.Collections;

/**
 * Class constraint
 */
public class WMIClassConstraint implements DBSEntityConstraint, DBSEntityReferrer, DBSEntityAttributeRef
{
    private final WMIClass owner;
    private final WMIClassAttribute key;

    public WMIClassConstraint(WMIClass owner, WMIClassAttribute key)
    {
        this.owner = owner;
        this.key = key;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSEntity getParentObject()
    {
        return owner;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return owner.getDataSource();
    }

    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.UNIQUE_KEY;
    }

    @Override
    public String getName()
    {
        return key.getName();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public Collection<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return Collections.singletonList(this);
    }

    @Override
    public DBSEntityAttribute getAttribute()
    {
        return key;
    }
}
