/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
