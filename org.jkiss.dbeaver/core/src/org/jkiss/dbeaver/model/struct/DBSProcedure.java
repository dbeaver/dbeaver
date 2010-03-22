package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;

import java.util.Collection;

/**
 * DBSIndex
 */
public interface DBSProcedure<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSStructureContainer<DATASOURCE>>
    extends DBSStructureObject<DATASOURCE>
{
    CONTAINER getContainer();

    DBSProcedureType getProcedureType();

    Collection<? extends DBSProcedureColumn> getColumns() throws DBException;

}