package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

/**
 * Abstract object reference
 */
public abstract class AbstractObjectReference implements DBSObjectReference {

    private final String name;
    private final String containerName;
    private final String description;
    private final DBSObjectType type;

    protected AbstractObjectReference(String name, String containerName, String description, DBSObjectType type)
    {
        this.name = name;
        this.containerName = containerName;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public String getContainerName()
    {
        return containerName;
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

}
