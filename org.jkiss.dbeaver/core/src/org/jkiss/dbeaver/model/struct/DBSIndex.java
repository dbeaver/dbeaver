package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

import java.util.Collection;

/**
 * DBSIndex
 */
public interface DBSIndex<DATASOURCE extends DBPDataSource> extends DBSStructureObject<DATASOURCE>
{
    DBSTable getTable();

    boolean isUnique();

    DBSIndexType getIndexType();

    Collection<? extends DBSIndexColumn> getColumns();

}
