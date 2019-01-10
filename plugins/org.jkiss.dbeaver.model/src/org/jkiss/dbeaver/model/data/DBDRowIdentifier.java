/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
