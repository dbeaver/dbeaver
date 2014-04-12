/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.List;

/**
 * Result set table metadata
 */
public interface DBCEntityMetaData {

    /**
     * Table name
     * @return table name
     */
    @NotNull
    String getEntityName();

    /**
     * Meta attributes which belongs to this entity
     */
    @NotNull
    List<? extends DBCAttributeMetaData> getAttributes();

    // TODO: move to utils
    /**
     * Table reference
     * @return table table reference. never returns null
     * @param monitor progress monitor
     */
    @Nullable
    DBSEntity getEntity(DBRProgressMonitor monitor)
        throws DBException;

    // TODO: move to utils
    @Nullable
    DBCAttributeMetaData getAttributeMetaData(DBRProgressMonitor monitor, DBSEntityAttribute column)
        throws DBException;

    // TODO: move to utils
    /**
     * Gets best table identifier.
     * Best identifier is a primary key. If no such one then any unique key fits.
     * @return list of identifier columns which identifies this table row the best way
     * or null if no identifiers found.
     * @param monitor progress monitor
     */
    @Nullable
    DBCEntityIdentifier getBestIdentifier(DBRProgressMonitor monitor)
        throws DBException;

}
