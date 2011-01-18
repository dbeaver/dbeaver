/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.project.DBPProjectHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * ProjectHandlerDescriptor
 */
public class ProjectHandlerDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.projectHandler";

    private String id;
    private String className;
    private List<String> sourceTypes = new ArrayList<String>();
    private String name;
    private String description;
    private String fileExtension;
    private Image icon;

    private Class<DBPProjectHandler> handlerClass;
    private DBPProjectHandler handler;

    public ProjectHandlerDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.id = config.getAttribute("id");
        this.className = config.getAttribute("class");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.fileExtension = config.getAttribute("extension");
        String iconPath = config.getAttribute("icon");
        if (!CommonUtils.isEmpty(iconPath)) {
            this.icon = iconToImage(iconPath);
        }

        IConfigurationElement[] typesCfg = config.getChildren("sourceType");
        if (typesCfg != null) {
            for (IConfigurationElement typeCfg : typesCfg) {
                String objectType = typeCfg.getAttribute("type");
                if (objectType != null) {
                    sourceTypes.add(objectType);
                }
            }
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

    public String getFileExtension()
    {
        return fileExtension;
    }

    public Image getIcon()
    {
        return icon;
    }

    public synchronized Class<DBPProjectHandler> getHandlerClass()
    {
        if (handlerClass == null) {
            handlerClass = (Class<DBPProjectHandler>)getObjectClass(className);
        }
        return handlerClass;
    }

    public synchronized DBPProjectHandler getHandler() throws IllegalAccessException, InstantiationException
    {
        if (handler == null) {
            Class<DBPProjectHandler> clazz = getHandlerClass();
            if (clazz == null) {
                return null;
            }
            handler = clazz.newInstance();
        }
        return handler;
    }

}
