/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
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
    int STRUCT_ENTITIES = 1;
    /**
     * Cache attributes of underlying entities/relations
     */
    int STRUCT_ATTRIBUTES = 2;
    /**
     * Cache underlying relations
     */
    int STRUCT_ASSOCIATIONS = 4;
    /**
     * Cache everything
     */
    int STRUCT_ALL = STRUCT_ENTITIES | STRUCT_ATTRIBUTES | STRUCT_ASSOCIATIONS;

    /**
     * Retrieve list of immediate child objects (not recursive)
     * @return collection of child objects (not null).
     *  Objects type depends on implementor (catalogs, schemas, tables, etc)
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets child object by its name.
     * In most cases object name have to be case insensitive.
     *
     * @param monitor progress monitor
     * @param childName name of child object
     * @throws DBException on any DB error
     * @return child object or null
     */
    DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException;

    /**
     * Gets type of child elements.
     *
     * @param monitor progress monitor
     * @return type of child objects
     * @throws org.jkiss.dbeaver.DBException on error
     */
    Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Caches all underlying structure contents.
     * Reads tables, columns, foreign keys and other RDB information.
     * This method is invoked when view want to draw something like ER diagramm which
     * includes all container entities.
     * @throws DBException on any DB error
     * @param monitor progress monitor
     * @param scope underlying structure scope
     */
    void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException;

}
