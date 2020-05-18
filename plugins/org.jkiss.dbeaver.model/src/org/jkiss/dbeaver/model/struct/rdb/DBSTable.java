/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.Collection;

/**
 * Table
 */
public interface DBSTable extends DBSEntity, DBPQualifiedObject
{

    boolean isView();

    /**
     * Table indices
     * @return list of indices
     * @throws DBException  on any DB error
     * @param monitor progress monitor
     */
    Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException;

    /**
     * Keys are: primary keys and unique keys.
     * Foreign keys can be obtained with {@link #getReferences(org.jkiss.dbeaver.model.runtime.DBRProgressMonitor)}
     * @return list of constraints
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    @Nullable
    @Override
    Collection<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets this table foreign keys
     * @return foreign keys list
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
//    @Override
//    Collection<? extends DBSTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets foreign keys which refers this table
     * @return foreign keys list
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
//    @Override
//    Collection<? extends DBSTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException;

}
