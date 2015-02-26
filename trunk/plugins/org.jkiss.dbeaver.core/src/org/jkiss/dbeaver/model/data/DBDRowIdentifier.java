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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.exec.DBCEntityIdentifier;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;

/**
 * Row identifier.
 * Unique identifier of row in certain table.
 */
public class DBDRowIdentifier implements DBPObject {

    private DBSEntity entity;
    private DBCEntityIdentifier entityIdentifier;

    public DBDRowIdentifier(@NotNull DBSEntity entity, @NotNull DBCEntityIdentifier entityIdentifier)
    {
        this.entity = entity;
        this.entityIdentifier = entityIdentifier;
    }

    @NotNull
    @Property(viewable = true, order = 1)
    public DBSEntity getEntity() {
        return entity;
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public DBSEntityConstraint getUniqueKey() {
        return entityIdentifier.getReferrer();
    }

    @NotNull
    public DBCEntityIdentifier getEntityIdentifier()
    {
        return entityIdentifier;
    }

    @NotNull
    public String getKeyType()
    {
        return entityIdentifier.getReferrer().getConstraintType().getName();
    }

/*
    public Object[] getKeyValues(Object[] row) {
        Object[] keyValues = new Object[keyColumns.size()];
        for (DBSTableColumn column : keyColumns) {
            keyColumns
        }
        return keyValues;
    }
*/

}
