/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSObjectContainer
 */
public interface DBSObjectContainer extends DBSObject
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
     * Cache everything
     */
    public static final int STRUCT_ALL = STRUCT_ENTITIES | STRUCT_ATTRIBUTES | STRUCT_ASSOCIATIONS;

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
     *
     * @param monitor progress monitor
     * @param childName name of child object
     * @throws DBException on any DB error
     * @return child object or null
     */
    DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException;

    /**
     * Gets type of child elements.
     *
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
     * @throws DBException on any DB error
     * @param monitor progress monitor
     * @param scope underlying structure scope
     */
    void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException;

}
