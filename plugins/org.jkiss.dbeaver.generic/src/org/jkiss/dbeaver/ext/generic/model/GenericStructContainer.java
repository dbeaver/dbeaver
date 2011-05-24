package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Generic struct container
 */
public interface GenericStructContainer extends DBSEntityContainer {

    GenericDataSource getDataSource();

    DBSObject getObject();

    GenericCatalog getCatalog();

    GenericSchema getSchema();

    TableCache getTableCache();

    IndexCache getIndexCache();

    PrimaryKeysCache getPrimaryKeysCache();

    ForeignKeysCache getForeignKeysCache();


    Collection<GenericTable> getTables(DBRProgressMonitor monitor) throws DBException;

    GenericTable getTable(DBRProgressMonitor monitor, String name) throws DBException;

    Collection<GenericIndex> getIndexes(DBRProgressMonitor monitor) throws DBException;

    Collection<GenericPackage> getPackages(DBRProgressMonitor monitor) throws DBException;

    Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException;

    Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name) throws DBException;
}
