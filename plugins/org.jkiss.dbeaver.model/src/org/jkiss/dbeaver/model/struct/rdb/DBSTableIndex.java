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
package org.jkiss.dbeaver.model.struct.rdb;

import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * DBSTableIndex
 */
public interface DBSTableIndex extends DBSEntityConstraint, DBSEntityReferrer, DBPQualifiedObject
{
    /**
     * Index container. In complex databases it is schema or catalog where index defined.
     * Also the table can be index container.
     * @return container
     */
    DBSObject getContainer();

    DBSTable getTable();

    boolean isUnique();

    DBSIndexType getIndexType();

    List<? extends DBSTableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor);

}
