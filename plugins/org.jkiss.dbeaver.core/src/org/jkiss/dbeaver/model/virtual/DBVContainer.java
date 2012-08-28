package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Virtual container
 */
public class DBVContainer implements DBSObject {

    private DBSObject parent;
    private String name;
    private String description;
    private String type;

    public DBVContainer(DBSObject parent, String name, String description, String type)
    {
        this.parent = parent;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    @Override
    public DBSObject getParentObject()
    {
        return parent;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return parent.getDataSource();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    public String getType()
    {
        return type;
    }
}
