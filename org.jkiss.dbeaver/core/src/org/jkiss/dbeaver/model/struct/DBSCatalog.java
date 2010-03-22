package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;

import java.util.Collection;

/**
 * DBSCatalog
 */
public interface DBSCatalog<DATASOURCE extends DBPDataSource>
    extends DBSStructureObject<DATASOURCE>, DBSStructureContainer<DATASOURCE>
{
    Collection<? extends DBSSchema> getSchemas() throws DBException;
}
