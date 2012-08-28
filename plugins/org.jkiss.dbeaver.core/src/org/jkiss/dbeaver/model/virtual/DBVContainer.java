package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Virtual container
 */
public class DBVContainer implements DBSObject {

    private final DBVContainer parent;
    private String name;
    private String description;
    private Map<String, DBVContainer> containers = new LinkedHashMap<String, DBVContainer>();
    private Map<String, DBVEntity> entities = new LinkedHashMap<String, DBVEntity>();

    public DBVContainer(DBVContainer parent, String name)
    {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public DBVContainer getParentObject()
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

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    public DBVContainer getContainer(String name)
    {
        String dictName = name.toLowerCase();
        DBVContainer container = containers.get(dictName);
        if (container == null) {
            container = new DBVContainer(this, name);
            containers.put(dictName, container);
        }
        return container;
    }

    public DBVEntity getEntity(String name)
    {
        String dictName = name.toLowerCase();
        DBVEntity entity = entities.get(dictName);
        if (entity == null) {
            entity = new DBVEntity(this, name, null);
            entities.put(dictName, entity);
        }
        return entity;
    }

}
