/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;

/**
 * ResourceHandlerDescriptor
 */
public class ResourceHandlerDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resourceHandler";

    private String className;
    private String resourceType;

    private Class<DBPResourceHandler> handlerClass;
    private DBPResourceHandler handler;

    ResourceHandlerDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.resourceType = config.getAttribute("type");
        this.className = config.getAttribute("class");
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
