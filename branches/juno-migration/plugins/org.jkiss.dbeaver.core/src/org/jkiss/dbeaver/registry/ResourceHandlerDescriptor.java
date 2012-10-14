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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ResourceHandlerDescriptor
 */
public class ResourceHandlerDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resourceHandler"; //$NON-NLS-1$
    public static final char EXTENSIONS_DELIMITER = ',';

    private String resourceType;
    private List<String> fileExtensions;

    private ObjectType handlerType;
    private DBPResourceHandler handler;
    private List<IContentType> contentTypes = new ArrayList<IContentType>();
    private List<ObjectType> resourceTypes = new ArrayList<ObjectType>();

    ResourceHandlerDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.resourceType = config.getAttribute(RegistryConstants.ATTR_TYPE);
        this.handlerType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        String extensionsString = config.getAttribute(RegistryConstants.ATTR_EXTENSIONS);
        if (!CommonUtils.isEmpty(extensionsString)) {
            this.fileExtensions = CommonUtils.splitString(extensionsString, EXTENSIONS_DELIMITER);
        } else {
            this.fileExtensions = Collections.emptyList();
        }
        for (IConfigurationElement contentTypeBinding : CommonUtils.safeArray(config.getChildren("contentTypeBinding"))) {
            String contentTypeId = contentTypeBinding.getAttribute("contentTypeId");
            if (!CommonUtils.isEmpty(contentTypeId)) {
                IContentType contentType = Platform.getContentTypeManager().getContentType(contentTypeId);
                if (contentType != null) {
                    contentTypes.add(contentType);
                } else {
                    log.warn("Content type '" + contentTypeId + "' not recognized");
                }
            }
        }
        for (IConfigurationElement resourceTypeBinding : CommonUtils.safeArray(config.getChildren("resourceTypeBinding"))) {
            String resourceType = resourceTypeBinding.getAttribute("resourceType");
            if (!CommonUtils.isEmpty(resourceType)) {
                resourceTypes.add(new ObjectType(resourceType));
            }
        }
    }

    void dispose()
    {
        this.handler = null;
        this.handlerType = null;
    }

    public String getResourceType()
    {
        return resourceType;
    }

    public List<String> getFileExtensions()
    {
        return fileExtensions;
    }

    public synchronized Class<DBPResourceHandler> getHandlerClass()
    {
        return handlerType.getObjectClass(DBPResourceHandler.class);
    }

    public synchronized DBPResourceHandler getHandler() throws IllegalAccessException, InstantiationException
    {
        if (handler == null) {
            Class<DBPResourceHandler> clazz = getHandlerClass();
            if (clazz == null) {
                return null;
            }
            handler = clazz.newInstance();
        }
        return handler;
    }

    public boolean canHandle(IResource resource)
    {
        if (!contentTypes.isEmpty() && resource instanceof IFile) {
            try {
                IContentType fileContentType = ((IFile) resource).getContentDescription().getContentType();
                if (fileContentType != null && contentTypes.contains(fileContentType)) {
                    return true;
                }
            } catch (CoreException e) {
                log.warn("Can't obtain content description for '" + resource.getName() + "'", e);
            }
        }
        if (!resourceTypes.isEmpty()) {
            for (ObjectType objectType : resourceTypes) {
                if (objectType.appliesTo(resource)) {
                    return true;
                }
            }
        }
        return false;
    }
}
