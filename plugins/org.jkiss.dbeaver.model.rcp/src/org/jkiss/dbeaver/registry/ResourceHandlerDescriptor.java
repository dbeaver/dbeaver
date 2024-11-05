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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.app.DBPResourceHandlerDescriptor;
import org.jkiss.dbeaver.model.app.DBPResourceTypeDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.ArrayUtils;

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

    public void dispose() {
        this.handler = null;
        this.handlerType = null;
    }

    public String getTypeId() {
        return typeId;
    }

    @NotNull
    @Override
    public DBPResourceTypeDescriptor getResourceType() {
        return ResourceTypeRegistry.getInstance().getResourceType(typeId);
    }

    @Nullable
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

        if (resourceType != null && isApplicableTo(resourceType, resource, testContent)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return typeId;
    }

    public boolean isApplicableTo(DBPResourceTypeDescriptor resourceType, @NotNull IResource resource, boolean testContent) {
        if (!resourceType.getContentTypes().isEmpty() && resource instanceof IFile) {
            if (testContent) {
                try {
                    IContentDescription contentDescription = ((IFile) resource).getContentDescription();
                    if (contentDescription != null) {
                        IContentType fileContentType = contentDescription.getContentType();
                        if (fileContentType != null && resourceType.getContentTypes().contains(fileContentType)) {
                            return true;
                        }
                    }
                } catch (CoreException e) {
                    log.debug("Can't obtain content description for '" + resource.getName() + "'", e);
                }
            }
            // Check for file extension
            String fileExtension = resource.getFileExtension();
            for (IContentType contentType : resourceType.getContentTypes()) {
                String[] ctExtensions = contentType.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
                if (!ArrayUtils.isEmpty(ctExtensions)) {
                    for (String ext : ctExtensions) {
                        if (ext.equalsIgnoreCase(fileExtension)) {
                            return true;
                        }
                    }
                }
            }
        }
        if (!resourceType.getResourceTypes().isEmpty()) {
            for (ObjectType objectType : resourceType.getResourceTypes()) {
                if (objectType.appliesTo(resource, null)) {
                    return true;
                }
            }
        }
        return false;
    }


}
