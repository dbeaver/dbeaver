package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.Collection;

/**
 * Inheritance
 */
public class WMIClassInheritance implements DBSForeignKey
{

    private WMIClass superClass;
    private WMIClass subClass;

    public WMIClassInheritance(WMIClass superClass, WMIClass subClass)
    {
        this.superClass = superClass;
        this.subClass = subClass;
    }

    public boolean isPersisted()
    {
        return true;
    }

    public String getName()
    {
        return subClass.getName() + " inherits " + superClass.getName();
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return subClass;
    }

    public DBPDataSource getDataSource()
    {
        return subClass.getDataSource();
    }

    public DBSEntity getAssociatedEntity()
    {
        return superClass;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }

    public String getFullQualifiedName()
    {
        return getName();
    }

    public DBSConstraintType getConstraintType()
    {
        return DBSConstraintType.INHERITANCE;
    }

    public DBSTable getTable()
    {
        return subClass;
    }

    public Collection<? extends DBSConstraintColumn> getColumns(DBRProgressMonitor monitor)
    {
        return null;
    }

    public DBSConstraintColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn)
    {
        return null;
    }

    public DBSConstraint getReferencedKey()
    {
        return null;
    }

    public DBSConstraintModifyRule getDeleteRule()
    {
        return DBSConstraintModifyRule.RESTRICT;
    }

    public DBSConstraintModifyRule getUpdateRule()
    {
        return DBSConstraintModifyRule.RESTRICT;
    }
}
