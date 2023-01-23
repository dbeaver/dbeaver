/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.app.DBPResourceHandlerDescriptor;
import org.jkiss.dbeaver.model.app.DBPResourceTypeDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

/**
 * ResourceHandlerDescriptor
 */
public class ResourceHandlerDescriptor extends AbstractDescriptor implements DBPResourceHandlerDescriptor {
    private static final Log log = Log.getLog(ResourceHandlerDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resourceHandler"; //$NON-NLS-1$

    private final String typeId;
    private ObjectType handlerType;
    private DBPResourceHandler handler;

    public ResourceHandlerDescriptor(IConfigurationElement config) {
        super(config);

        this.typeId = config.getAttribute(RegistryConstants.ATTR_TYPE);
        this.handlerType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
    }

    void dispose() {
        this.handler = null;
        this.handlerType = null;
    }

    public String getTypeId() {
        return typeId;
    }

    @Override
    public DBPResourceTypeDescriptor getResourceType() {
        return ResourceTypeRegistry.getInstance().getResourceType(typeId);
    }

    public synchronized DBPResourceHandler getHandler() {
        if (handler == null) {
            Class<? extends DBPResourceHandler> clazz = handlerType.getObjectClass(DBPResourceHandler.class);
            if (clazz == null) {
                return null;
            }
            try {
                handler = clazz.getConstructor().newInstance();
            } catch (Exception e) {
                log.error("Can't instantiate resource handler", e);
            }
        }
        return handler;
    }

    public boolean isDefault() {
        return "default".equals(typeId);
    }

    public boolean canHandle(IResource resource) {
        return canHandle(resource, false);
    }

    public boolean canHandle(IResource resource, boolean testContent) {
        if (isDefault()) {
            return false;
        }
        DBPResourceTypeDescriptor resourceType = getResourceType();

        if (resourceType != null && resourceType.isApplicableTo(resource, testContent)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return typeId;
    }

}
