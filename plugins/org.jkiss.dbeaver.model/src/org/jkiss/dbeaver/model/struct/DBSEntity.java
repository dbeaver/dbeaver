/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSEntity
 */
public interface DBSEntity extends DBSObject
{
    /**
     * Entity type
     * @return entity type
     */
    @NotNull
    DBSEntityType getEntityType();

    /**
     * Gets this entity attributes
     * @return attribute list
     * @throws org.jkiss.dbeaver.DBException on any DB error
     * @param monitor progress monitor
     */
    @Nullable
    Collection<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Retrieve attribute by it's name (case insensitive)
     * @param monitor progress monitor
     * @param attributeName column name  @return column or null
     * @throws DBException on any DB error
     */
    @Nullable
    DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException;

    /**
     * Gets this entity constraints
     * @return association list
     * @throws org.jkiss.dbeaver.DBException on any DB error
     * @param monitor progress monitor
     */
    @Nullable
    Collection<? extends DBSEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets this entity associations
     * @return association list
     * @throws org.jkiss.dbeaver.DBException on any DB error
     * @param monitor progress monitor
     */
    @Nullable
    Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets associations which refers this entity
     * @return reference association list
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    @Nullable
    Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException;

}
