/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.impl;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * AbstractContextDescriptor
 */
public abstract class AbstractContextDescriptor extends AbstractDescriptor {
    private static final String OBJECT_TYPE = "objectType";

    @NotNull
    private final ObjectType[] objectTypes;

    public AbstractContextDescriptor(@NotNull IConfigurationElement config) {
        super(config.getContributor().getName());

        List<ObjectType> objectTypes = new ArrayList<>();
        String objectType = config.getAttribute(OBJECT_TYPE);
        if (objectType != null) {
            objectTypes.add(new ObjectType(objectType));
        }
        for (IConfigurationElement typeCfg : config.getChildren(OBJECT_TYPE)) {
            objectTypes.add(new ObjectType(typeCfg));
        }
        this.objectTypes = objectTypes.toArray(new ObjectType[0]);
    }

    public AbstractContextDescriptor(@NotNull String pluginId) {
        super(pluginId);
        this.objectTypes = new ObjectType[0];
    }

    public boolean hasObjectTypes() {
        return objectTypes.length > 0;
    }

    @NotNull
    public ObjectType[] getObjectTypes() {
        return objectTypes;
    }

    public boolean appliesTo(@NotNull DBPObject object) {
        return appliesTo(object, null);
    }

    public boolean matchesType(@NotNull Class<? extends DBSObject> objectClass) {
        for (ObjectType objectType : objectTypes) {
            if (objectType.matchesType(objectClass)) {
                return true;
            }
        }
        return false;
    }

    public boolean appliesTo(@NotNull DBPObject object, @Nullable Object context) {
        if (object instanceof DBSObject) {
            object = DBUtils.getPublicObject((DBSObject) object);
        }
        if (object == null) {
            return false;
        }
        Object adapted = adaptType(object);
        for (ObjectType objectType : objectTypes) {
            if (objectType.appliesTo(object, context) || (adapted != null && objectType.appliesTo(adapted, context))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    protected Object adaptType(@NotNull DBPObject object) {
        return null;
    }
}
