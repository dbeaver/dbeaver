/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;

/**
 * ResourceHandlerDescriptor
 */
public class ResourceHandlerDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resourceHandler";

    private String id;
    private String className;
    private String name;
    private String description;
    private Image icon;

    private Class<DBPResourceHandler> handlerClass;
    private DBPResourceHandler handler;
    private String resourceType;

    ResourceHandlerDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.id = config.getAttribute("id");
        this.resourceType = config.getAttribute("type");
        this.className = config.getAttribute("class");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        String iconPath = config.getAttribute("icon");
        if (!CommonUtils.isEmpty(iconPath)) {
            this.icon = iconToImage(iconPath);
        }
    }

    void dispose()
    {
        this.handler = null;
        this.handlerClass = null;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public Image getIcon()
    {
        return icon;
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
