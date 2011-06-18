/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;

import java.util.Collections;
import java.util.List;

/**
 * ResourceHandlerDescriptor
 */
public class ResourceHandlerDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resourceHandler";

    private String className;
    private String resourceType;
    private List<String> fileExtensions;

    private Class<DBPResourceHandler> handlerClass;
    private DBPResourceHandler handler;

    ResourceHandlerDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.resourceType = config.getAttribute("type");
        this.className = config.getAttribute("class");
        String extensionsString = config.getAttribute("extensions");
        if (!CommonUtils.isEmpty(extensionsString)) {
            this.fileExtensions = CommonUtils.splitString(extensionsString, ',');
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
            handlerClass = (Class<DBPResourceHandler>)getObjectClass(className);
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
