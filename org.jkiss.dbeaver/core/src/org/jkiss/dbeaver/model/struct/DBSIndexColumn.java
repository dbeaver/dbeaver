package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSIndex
 */
public interface DBSIndexColumn<DATASOURCE extends DBPDataSource> extends DBSStructureObject<DATASOURCE>
{
    DBSIndex getIndex();

    int getOrdinalPosition();

    boolean isAscending();

    DBSTableColumn getTableColumn();

}