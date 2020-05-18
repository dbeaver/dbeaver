/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.runtime.serialize;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.meta.DBSerializable;

import java.util.HashMap;
import java.util.Map;

public class SerializerRegistry
{
    private static final Log log = Log.getLog(SerializerRegistry.class);

    private static SerializerRegistry instance = null;

    private final Map<String, SerializerDescriptor> serializers = new HashMap<>();

    public synchronized static SerializerRegistry getInstance()
    {
        if (instance == null) {
            instance = new SerializerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private SerializerRegistry(IExtensionRegistry registry)
    {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(SerializerDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            // Load main nodes
            if ("serializer".equals(ext.getName())) {
                SerializerDescriptor sd = new SerializerDescriptor(ext);
                serializers.put(sd.getId(), sd);
            }
        }
    }

    public boolean isSerializable(Object object) {
        return object.getClass().getAnnotation(DBSerializable.class) != null;
    }

    public String getObjectType(Object object) {
        DBSerializable dbSerializable = object.getClass().getAnnotation(DBSerializable.class);
        return dbSerializable != null ? dbSerializable.value() : null;
    }

    @Nullable
    public DBPObjectSerializer createSerializer(Object object) {
        DBSerializable dbSerializable = object.getClass().getAnnotation(DBSerializable.class);
        if (dbSerializable != null) {
            return createSerializerByType(dbSerializable.value());
        }
        return null;
    }

    @Nullable
    public <OBJECT_CONTEXT, OBJECT_TYPE> DBPObjectSerializer<OBJECT_CONTEXT, OBJECT_TYPE> createSerializerByType(String typeID) {
        SerializerDescriptor sd = serializers.get(typeID);
        if (sd == null) {
            log.error("Serializer '" + typeID + "' not found");
            return null;
        }
        try {
            return sd.createSerializer();
        } catch (Exception e) {
            log.error("Error creating serializer " + sd.getId(), e);
            return null;
        }
    }

}
