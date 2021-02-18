/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;
import java.util.List;

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
    List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException;

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
