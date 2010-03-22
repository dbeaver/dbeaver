package org.jkiss.dbeaver.model.impl.meta;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSPrimaryKey;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;
import org.jkiss.dbeaver.model.struct.DBSTable;

/**
 * AbstractPrimaryKey
 */
public abstract class AbstractPrimaryKey<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSStructureContainer<DATASOURCE>,
    TABLE extends DBSTable<DATASOURCE, CONTAINER>>
    extends AbstractConstraint<DATASOURCE, CONTAINER, TABLE>
    implements DBSPrimaryKey<DATASOURCE, TABLE>
{

    public AbstractPrimaryKey(
        TABLE table,
        String name,
        String description)
    {
        super(table, name, description);
    }

}