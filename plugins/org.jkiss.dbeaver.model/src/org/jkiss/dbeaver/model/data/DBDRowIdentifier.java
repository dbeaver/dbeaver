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
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Row identifier.
 * Unique identifier of row in certain table.
 */
public class DBDRowIdentifier implements DBPObject {

    private final DBSEntity entity;
    private final DBSEntityConstraint entityIdentifier;
    private final List<DBDAttributeBinding> attributes = new ArrayList<>();

    public DBDRowIdentifier(@NotNull DBSEntity entity, @NotNull DBSEntityConstraint entityIdentifier)
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
        return entityIdentifier;
    }

    @NotNull
    public String getKeyType()
    {
        return entityIdentifier.getConstraintType().getName();
    }

    public boolean isIncomplete() {
        return attributes.isEmpty();
    }

    @NotNull
    public List<DBDAttributeBinding> getAttributes() {
        return attributes;
    }

    public boolean isValidIdentifier() {
        if (entityIdentifier instanceof DBSEntityReferrer && CommonUtils.isEmpty(attributes)) {
            return false;
        }
        return true;
    }

    public boolean hasAttribute(DBDAttributeBinding attributeBinding) {
        return attributes.contains(attributeBinding);
    }

    public void reloadAttributes(@NotNull DBRProgressMonitor monitor, @NotNull DBDAttributeBinding[] bindings) throws DBException
    {
        this.attributes.clear();
        if (entityIdentifier instanceof DBVEntityConstraint && ((DBVEntityConstraint) entityIdentifier).isUseAllColumns()) {
            Collections.addAll(this.attributes, bindings);
        } else if (entityIdentifier instanceof DBSEntityReferrer) {
            DBSEntityReferrer referrer = (DBSEntityReferrer) entityIdentifier;
            Collection<? extends DBSEntityAttributeRef> refs = CommonUtils.safeCollection(referrer.getAttributeReferences(monitor));
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

    public void clearAttributes() {
        attributes.clear();
    }

    @Override
    public String toString() {
        return entity.getName() + "." + entityIdentifier.getName() + "(" +
            attributes.stream().map(DBDAttributeBinding::getName).collect(Collectors.joining(",")) + ")";
    }

}
