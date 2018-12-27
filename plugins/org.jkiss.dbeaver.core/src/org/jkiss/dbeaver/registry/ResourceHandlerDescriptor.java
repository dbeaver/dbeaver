/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.app.DBPResourceHandlerDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ResourceHandlerDescriptor
 */
public class ResourceHandlerDescriptor extends AbstractDescriptor implements DBPResourceHandlerDescriptor
{
    private static final Log log = Log.getLog(ResourceHandlerDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resourceHandler"; //$NON-NLS-1$

    private String id;
    private String name;
    private boolean managable;
    private DBPImage icon;
    private ObjectType handlerType;
    private DBPResourceHandler handler;
    private List<IContentType> contentTypes = new ArrayList<>();
    private List<ObjectType> resourceTypes = new ArrayList<>();
    private List<String> roots = new ArrayList<>();
    private String defaultRoot;

    ResourceHandlerDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.managable = CommonUtils.toBoolean(config.getAttribute(RegistryConstants.ATTR_MANAGABLE));
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
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

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isManagable() {
        return managable;
    }

    @Override
    public DBPImage getIcon() {
        return icon;
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
                log.debug("Can't obtain content description for '" + resource.getName() + "'", e);
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

    public String getDefaultRoot(IProject project)
    {
        try {
            IEclipsePreferences resourceHandlers = RuntimeUtils.getResourceHandlerPreferences(project, DBPResourceHandlerDescriptor.RESOURCE_ROOT_FOLDER_NODE);
            return resourceHandlers.get(id, defaultRoot);
        } catch (Exception e) {
            log.error("Can't obtain resource handler preferences", e);
            return null;
        }
    }

    public List<String> getRoots()
    {
        return roots;
    }

    @Override
    public String toString() {
        return id;
    }
}
