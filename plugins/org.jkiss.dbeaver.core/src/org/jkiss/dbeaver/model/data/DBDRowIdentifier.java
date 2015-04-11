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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Row identifier.
 * Unique identifier of row in certain table.
 */
public class DBDRowIdentifier implements DBPObject {

    private final DBSEntity entity;
    private final DBSEntityReferrer entityIdentifier;
    private final List<DBDAttributeBinding> attributes = new ArrayList<DBDAttributeBinding>();

    public DBDRowIdentifier(@NotNull DBSEntity entity, @NotNull DBSEntityReferrer entityIdentifier)
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
    public DBSEntityReferrer getUniqueKey() {
        return entityIdentifier;
    }

    @NotNull
    public String getKeyType()
    {
        return entityIdentifier.getConstraintType().getName();
    }

    @NotNull
    public List<DBDAttributeBinding> getAttributes() {
        return attributes;
    }

    public void reloadAttributes(@NotNull DBRProgressMonitor monitor, @NotNull DBDAttributeBinding[] bindings) throws DBException
    {
        this.attributes.clear();
        Collection<? extends DBSEntityAttributeRef> refs = CommonUtils.safeCollection(entityIdentifier.getAttributeReferences(monitor));
        for (DBSEntityAttributeRef cColumn : refs) {
            DBDAttributeBinding binding = DBUtils.findBinding(bindings, cColumn.getAttribute());
            if (binding != null) {
                this.attributes.add(binding);
            } else {
                // If at least one attribute is missing - this ID won't work anyway
                // so let's just clean it up
                this.attributes.clear();
                break;
            }
        }
    }

}
