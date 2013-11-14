package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

/**
 * Abstract object reference
 */
public abstract class AbstractObjectReference implements DBSObjectReference {

    private final String name;
    private final DBSObject container;
    private final String description;
    private final DBSObjectType type;

    protected AbstractObjectReference(String name, DBSObject container, String description, DBSObjectType type)
    {
        this.name = name;
        this.container = container;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public DBSObject getContainer()
    {
        return container;
    }

    @Override
    public String getObjectDescription()
    {
        return description;
    }

    @Override
    public DBSObjectType getObjectType()
    {
        return type;
    }

    @Override
    public String getFullQualifiedName()
    {
        DBPDataSource dataSource = container.getDataSource();
        if (container == dataSource) {
            // In case if there are no schemas/catalogs supported
            // and data source is a root container
            return DBUtils.getQuotedIdentifier(dataSource, name);
        }
        return DBUtils.getFullQualifiedName(dataSource, container) +
            dataSource.getInfo().getStructSeparator() +
            DBUtils.getQuotedIdentifier(dataSource, name);

    }
}
