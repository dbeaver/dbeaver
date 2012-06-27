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

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;

/**
 * ResourceHandlerDescriptor
 */
public class ResourceHandlerDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resourceHandler"; //$NON-NLS-1$
    public static final char EXTENSIONS_DELIMITER = ',';

    private String className;
    private String resourceType;
    private List<String> fileExtensions;

    private Class<DBPResourceHandler> handlerClass;
    private DBPResourceHandler handler;

    ResourceHandlerDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.resourceType = config.getAttribute(RegistryConstants.ATTR_TYPE);
        this.className = config.getAttribute(RegistryConstants.ATTR_CLASS);
        String extensionsString = config.getAttribute(RegistryConstants.ATTR_EXTENSIONS);
        if (!CommonUtils.isEmpty(extensionsString)) {
            this.fileExtensions = CommonUtils.splitString(extensionsString, EXTENSIONS_DELIMITER);
        } else {
            this.fileExtensions = Collections.emptyList();
        }
    }

    void dispose()
    {
        this.handler = null;
        this.handlerClass = null;
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
        if (handlerClass == null) {
            handlerClass = getObjectClass(className, DBPResourceHandler.class);
        }
        return handlerClass;
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

}
