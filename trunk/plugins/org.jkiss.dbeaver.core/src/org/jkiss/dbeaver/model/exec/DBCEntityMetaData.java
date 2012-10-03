/*
 * Copyright (C) 2010-2012 Serge Rieder
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

/**
 * Result set table metadata
 */
public interface DBCEntityMetaData {

    /**
     * Table reference
     * @return table table reference. never returns null
     * @param monitor progress monitor
     */
    DBSEntity getEntity(DBRProgressMonitor monitor)
        throws DBException;

    /**
     * Table name
     * @return table name
     */
    String getEntityName();

    /**
     * Table alias
     * @return table alias in query
     */
    String getEntityAlias();

    /**
     * Checks table is identified.
     * Table is identified if resultset contains at least one set of this table columns which will unique
     * identify table row
     * @return true if this table has at least one unique identifier in the whole resultset.
     * @param monitor progress monitor
     */
    boolean isIdentified(DBRProgressMonitor monitor)
        throws DBException;

    /**
     * Gets best table identifier.
     * Best identifier is a primary key. If no such one then any unique key fits.
     * @return list of identifier columns which identifies this table row the best way
     * or null if no identifiers found.
     * @param monitor progress monitor
     */
    DBCEntityIdentifier getBestIdentifier(DBRProgressMonitor monitor)
        throws DBException;

    DBCAttributeMetaData getColumnMetaData(DBRProgressMonitor monitor, DBSEntityAttribute column)
        throws DBException;
}
