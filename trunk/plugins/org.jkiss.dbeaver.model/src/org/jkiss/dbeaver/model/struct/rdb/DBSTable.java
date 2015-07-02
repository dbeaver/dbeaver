/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.struct.rdb;

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
    Collection<? extends DBSTableConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets this table foreign keys
     * @return foreign keys list
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    @Override
    Collection<? extends DBSTableForeignKey> getAssociations(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets foreign keys which refers this table
     * @return foreign keys list
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    @Override
    Collection<? extends DBSTableForeignKey> getReferences(DBRProgressMonitor monitor) throws DBException;

}
