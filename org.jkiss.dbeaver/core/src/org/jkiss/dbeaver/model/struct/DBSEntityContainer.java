/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSEntityContainer
 */
public interface DBSEntityContainer extends DBSObject
{
    /**
     * Cache underlying entities
     */
    public static final int STRUCT_ENTITIES = 1;
    /**
     * Cache attributes of underlying entities/relations
     */
    public static final int STRUCT_ATTRIBUTES = 2;
    /**
     * Cache underlying relations
     */
    public static final int STRUCT_ASSOCIATIONS = 4;

    /**
     * Retrieve list of immediate child objects (not recursive)
     * @return collection of child objects (not null).
     *  Objects type depends on implementor (catalogs, schemas, tables, etc)
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets child object by its name.
     * In most cases object name have to be case insensitive.
     * @param monitor progress monitor
     * @param childName name of child object  @return child object or null
     * @throws DBException on any DB error
     * @return child object or null
     */
    DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException;

    /**
     * Gets type of child elements.
     * @param monitor progress monitor
     * @return type of child objects
     * @throws org.jkiss.dbeaver.DBException on error
     */
    Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor) throws DBException;

    /**
     * Caches all underlying structure contents.
     * Reads tables, columns, foreign keys and other RDB information.
     * This method is invoked when view want to draw something like ER diagramm which
     * includes all container entities.
     * @throws DBException on any DB error  @param monitor
     * @param monitor progress monitor
     * @param scope underlying structure scope
     */
    void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException;

}
