package org.jkiss.dbeaver.model.impl.meta;

import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.DBException;

import java.util.Collection;

/**
 * GenericConstraint
 */
public abstract class AbstractConstraint<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSStructureContainer<DATASOURCE>,
    TABLE extends DBSTable<DATASOURCE, CONTAINER>>
    implements DBSConstraint<DATASOURCE, TABLE>
{
    private TABLE table;
    private String name;
    protected String description;

    protected AbstractConstraint(TABLE table, String name, String description)
    {
        this.table = table;
        this.name = name;
        this.description = description;
    }

    @Property(name = "Owner", viewable = true, order = 2)
    public TABLE getTable()
    {
        return table;
    }

    public DBSConstraintColumn<DATASOURCE> getColumn(DBSTableColumn tableColumn)
    {
        Collection<? extends DBSConstraintColumn> columns = getColumns();
        for (DBSConstraintColumn constraintColumn : columns) {
            if (constraintColumn.getTableColumn() == tableColumn) {
                return constraintColumn;
            }
        }
        return null;
    }

    @Property(name = "Name", viewable = false, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    public DBSObject getParentObject()
    {
        return table;
    }

    public DATASOURCE getDataSource()
    {
        return table.getDataSource();
    }

    public boolean refreshObject()
        throws DBException
    {
        return false;
    }

    public String toString()
    {
        return getName() == null ? "<NONE>" : getName();
    }

}