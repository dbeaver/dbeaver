package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;

import java.util.Collection;

/**
 * DBSIndex
 */
public interface DBSProcedure extends DBSStructureObject
{
    DBSStructureContainer getContainer();

    DBSProcedureType getProcedureType();

    Collection<? extends DBSProcedureColumn> getColumns() throws DBException;

}