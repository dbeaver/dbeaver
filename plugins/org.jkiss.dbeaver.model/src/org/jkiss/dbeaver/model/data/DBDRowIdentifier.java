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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
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
    private final List<DBDAttributeBinding> attributes = new ArrayList<>();

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
