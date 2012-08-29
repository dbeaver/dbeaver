package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Virtual container
 */
public class DBVContainer extends DBVObject implements DBSObject {

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

    public Collection<DBVContainer> getContainers() {
        return containers.values();
    }

    public DBVContainer getContainer(String name)
    {
        DBVContainer container = containers.get(name.toLowerCase());
        if (container == null) {
            container = new DBVContainer(this, name);
            addContainer(container);
        }
        return container;
    }

    void addContainer(DBVContainer container)
    {
        containers.put(container.getName().toLowerCase(), container);
    }

    public Collection<DBVEntity> getEntities() {
        return entities.values();
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

    void addEntity(DBVEntity entity)
    {
        entities.put(entity.getName().toLowerCase(), entity);
    }

    public void persist(XMLBuilder xml) throws IOException
    {
        if (!this.hasValuableData()) {
            // nothing to save
            return;
        }
        xml.startElement(RegistryConstants.TAG_CONTAINER);
        xml.addAttribute(RegistryConstants.ATTR_NAME, getName());
        // Containers
        for (DBVContainer container : getContainers()) {
            container.persist(xml);
        }

        for (DBVEntity entity : getEntities()) {
            entity.persist(xml);
        }

        xml.endElement();
    }

    public boolean hasValuableData() {
        for (DBVEntity entity : getEntities()) {
            if (entity.hasValuableData()) {
                return true;
            }
        }
        for (DBVContainer child : getContainers()) {
            if (child.hasValuableData()) {
                return true;
            }
        }
        return false;
    }

    void copyFrom(DBVContainer container) {
        this.name = container.name;
        this.description = container.description;

        this.containers.clear();
        for (DBVContainer child : container.getContainers()) {
            DBVContainer myChild = new DBVContainer(this, child.getName());
            myChild.copyFrom(child);
            containers.put(myChild.getName().toLowerCase(), myChild);
        }

        this.entities.clear();
        for (DBVEntity child : container.getEntities()) {
            DBVEntity myChild = new DBVEntity(this, child.getName(), child.getDescriptionColumnNames());
            myChild.copyFrom(child);
            entities.put(myChild.getName().toLowerCase(), myChild);
        }
    }

}
