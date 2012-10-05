package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Virtual constraint
 */
public class DBVEntityConstraint implements DBSEntityConstraint, DBSEntityReferrer
{
    private final DBVEntity entity;
    private final List<DBVEntityConstraintColumn> attributes = new ArrayList<DBVEntityConstraintColumn>();
    private DBSEntityConstraintType type;
    private String name;

    public DBVEntityConstraint(DBVEntity entity, DBSEntityConstraintType type, String name)
    {
        this.entity = entity;
        this.type = type;
        this.name = (name == null ? type.getName() : name);
    }

    @Override
    public Collection<DBVEntityConstraintColumn> getAttributeReferences(DBRProgressMonitor monitor)
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
        return type;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    public boolean hasAttributes()
    {
        return !attributes.isEmpty();
    }

    public void setAttributes(Collection<DBSEntityAttribute> realAttributes)
    {
        attributes.clear();
        for (DBSEntityAttribute attr : realAttributes) {
            attributes.add(new DBVEntityConstraintColumn(this, attr.getName()));
        }
    }

    public void addAttribute(String name)
    {
        attributes.add(new DBVEntityConstraintColumn(this, name));
    }

    void copyFrom(DBVEntityConstraint constraint)
    {
        this.attributes.clear();
        for (DBVEntityConstraintColumn col : constraint.attributes) {
            this.attributes.add(new DBVEntityConstraintColumn(this, col.getAttributeName()));
        }
    }
}
