package org.jkiss.dbeaver.model.impl.meta;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSProcedure;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;
import org.jkiss.dbeaver.DBException;

/**
 * AbstractProcedure
 */
public abstract class AbstractProcedure<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSStructureContainer>
    implements DBSProcedure
{
    private CONTAINER container;
    private String name;
    private String description;

    protected AbstractProcedure(CONTAINER container)
    {
        this.container = container;
    }

    protected AbstractProcedure(CONTAINER container, String name, String description)
    {
        this(container);
        this.name = name;
        this.description = description;
    }

    public CONTAINER getContainer()
    {
        return container;
    }

    @Property(name = "Procedure Name", order = 1)
    public String getName()
    {
        return name;
    }

    protected void setName(String tableName)
    {
        this.name = tableName;
    }

    @Property(name = "Procedure Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    public DATASOURCE getDataSource()
    {
        return (DATASOURCE) container.getDataSource();
    }

    public DBSObject getParentObject()
    {
        return container;
    }

    public boolean refreshObject()
        throws DBException
    {
        return false;
    }
}
