package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSSchema
 */
public interface DBSSchema<DATASOURCE extends DBPDataSource>
    extends DBSStructureObject<DATASOURCE>, DBSStructureContainer<DATASOURCE>
{
    DBSCatalog getCatalog();
}
