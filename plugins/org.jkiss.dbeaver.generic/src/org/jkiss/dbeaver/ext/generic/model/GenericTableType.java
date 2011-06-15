package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Table type
 */
public class GenericTableType implements DBSObject {

    private GenericDataSource dataSource;
    private String name;

    public GenericTableType(GenericDataSource dataSource, String name)
    {
        this.dataSource = dataSource;
        this.name = name;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    public String getName()
    {
        return name;
    }

    public boolean isPersisted()
    {
        return true;
    }
}
