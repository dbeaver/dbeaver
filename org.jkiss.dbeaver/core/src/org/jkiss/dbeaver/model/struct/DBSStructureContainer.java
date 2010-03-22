package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;

import java.util.Collection;

/**
 * DBSStructureContainer
 */
public interface DBSStructureContainer<DATASOURCE extends DBPDataSource> extends DBSObject<DATASOURCE>
{
    /**
     * Retrieve list of immediate child objects (not recursive)
     * @return collection of child objects (not null).
     *  Objects type depends on implementor (catalogs, schemas, tables, etc)
     * @throws DBException on any DB error
     */
    Collection<? extends DBSObject> getChildren() throws DBException;

    /**
     * Gets child object by its name.
     * In most cases object name have to be case insensitive.
     * @param childName name of child object
     * @return child object or null
     * @throws DBException on any DB error
     */
    DBSObject getChild(String childName) throws DBException;

    /**
     * Retrieve list of underlying tables
     * @return table collection (not null)
     * @throws DBException on any DB error
     */
    //Collection<? extends DBSTable> getTables() throws DBException;

    /**
     * Caches all underlying structure contents.
     * Reads tables, columns, foreign keys and other RDB information.
     * This method is invoked when view want to draw something like ER diagramm which
     * includes all container entities.
     * @throws DBException on any DB error
     */
    void cacheStructure() throws DBException;

}
