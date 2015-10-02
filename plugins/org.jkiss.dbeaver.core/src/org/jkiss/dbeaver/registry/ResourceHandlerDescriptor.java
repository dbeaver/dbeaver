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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ResourceHandlerDescriptor
 */
public class ResourceHandlerDescriptor extends AbstractDescriptor
{
    static final Log log = Log.getLog(ResourceHandlerDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resourceHandler"; //$NON-NLS-1$

    private ObjectType handlerType;
    private DBPResourceHandler handler;
    private List<IContentType> contentTypes = new ArrayList<>();
    private List<ObjectType> resourceTypes = new ArrayList<>();
    private List<String> roots = new ArrayList<>();
    private String defaultRoot;

    ResourceHandlerDescriptor(IConfigurationElement config)
    {
        super(config);

        this.handlerType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        for (IConfigurationElement contentTypeBinding : ArrayUtils.safeArray(config.getChildren("contentTypeBinding"))) {
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
        for (IConfigurationElement resourceTypeBinding : ArrayUtils.safeArray(config.getChildren("resourceTypeBinding"))) {
            String resourceType = resourceTypeBinding.getAttribute("resourceType");
            if (!CommonUtils.isEmpty(resourceType)) {
                resourceTypes.add(new ObjectType(resourceType));
            }
        }
        for (IConfigurationElement rootConfig : ArrayUtils.safeArray(config.getChildren("root"))) {
            String folder = rootConfig.getAttribute("folder");
            if (!CommonUtils.isEmpty(folder)) {
                roots.add(folder);
            }
            if ("true".equals(rootConfig.getAttribute("default"))) {
                defaultRoot = folder;
            }
        }
        if (CommonUtils.isEmpty(defaultRoot) && !CommonUtils.isEmpty(roots)) {
            defaultRoot = roots.get(0);
        }
    }

    void dispose()
    {
        this.handler = null;
        this.handlerType = null;
    }

    public synchronized DBPResourceHandler getHandler()
    {
        if (handler == null) {
            Class<? extends DBPResourceHandler> clazz = handlerType.getObjectClass(DBPResourceHandler.class);
            if (clazz == null) {
                return null;
            }
            try {
                handler = clazz.newInstance();
            } catch (Exception e) {
                log.error("Can't instantiate resource handler", e);
            }
        }
        return handler;
    }

    public boolean canHandle(IResource resource)
    {
        if (!contentTypes.isEmpty() && resource instanceof IFile) {
            try {
                IContentDescription contentDescription = ((IFile) resource).getContentDescription();
                if (contentDescription != null) {
                    IContentType fileContentType = contentDescription.getContentType();
                    if (fileContentType != null && contentTypes.contains(fileContentType)) {
                        return true;
                    }
                }
            } catch (CoreException e) {
                log.warn("Can't obtain content description for '" + resource.getName() + "'", e);
            }
            // Check for file extension
            String fileExtension = resource.getFileExtension();
            for (IContentType contentType : contentTypes) {
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
        if (!resourceTypes.isEmpty()) {
            for (ObjectType objectType : resourceTypes) {
                if (objectType.appliesTo(resource, null)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Collection<IContentType> getContentTypes()
    {
        return contentTypes;
    }

    public Collection<ObjectType> getResourceTypes()
    {
        return resourceTypes;
    }

    public String getDefaultRoot()
    {
        return defaultRoot;
    }

    public List<String> getRoots()
    {
        return roots;
    }

}
