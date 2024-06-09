/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.struct.rdb;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.dpi.DPIElement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.Collection;
import java.util.List;

/**
 * Table
 */
public interface DBSTable extends DBSEntity, DBPQualifiedObject {

    @DPIElement(cache = true)
    boolean isView();

    /**
     * Table indices
     *
     * @param monitor progress monitor
     * @return list of indices
     * @throws DBException on any DB error
     */
    Collection<? extends DBSTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Keys are: primary keys and unique keys.
     * Foreign keys can be obtained with {@link #getReferences(org.jkiss.dbeaver.model.runtime.DBRProgressMonitor)}
     *
     * @param monitor progress monitor
     * @return list of constraints
     * @throws DBException on any DB error
     */
    @Nullable
    @Override
    Collection<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets foreign keys which refers this table
     *
     * @param monitor progress monitor
     * @return foreign keys list
     * @throws DBException on any DB error
     */
    @Nullable
    List<? extends DBSTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException;

}
