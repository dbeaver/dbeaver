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
    DBSEntityType getEntityType();

    /**
     * Gets this entity attributes
     * @return attribute list
     * @throws org.jkiss.dbeaver.DBException on any DB error
     * @param monitor progress monitor
     */
    @Nullable
    Collection<? extends DBSEntityAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException;

    /**
     * Retrieve attribute by it's name (case insensitive)
     * @param monitor progress monitor
     * @param attributeName column name  @return column or null
     * @throws DBException on any DB error
     */
    @Nullable
    DBSEntityAttribute getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException;

    /**
     * Gets this entity constraints
     * @return association list
     * @throws org.jkiss.dbeaver.DBException on any DB error
     * @param monitor progress monitor
     */
    @Nullable
    Collection<? extends DBSEntityConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets this entity associations
     * @return association list
     * @throws org.jkiss.dbeaver.DBException on any DB error
     * @param monitor progress monitor
     */
    @Nullable
    Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets associations which refers this entity
     * @return reference association list
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    @Nullable
    Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException;

}
