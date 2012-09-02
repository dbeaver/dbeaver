package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Virtual unique constraint
 */
public class DBVEntityConstraint implements DBSEntityConstraint, DBSEntityReferrer
{
    private final DBVEntity entity;
    private final List<DBVEntityConstraintColumn> attributes = new ArrayList<DBVEntityConstraintColumn>();

    public DBVEntityConstraint(DBVEntity entity)
    {
        this.entity = entity;
    }

    @Override
    public Collection<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return attributes;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBVEntity getParentObject()
    {
        return entity;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return entity.getDataSource();
    }

    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.VIRTUAL_KEY;
    }

    @Override
    public String getName()
    {
        return "VIRTUAL";
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }
}
