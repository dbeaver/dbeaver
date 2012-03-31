/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.utils.CommonUtils;

/**
 * NetworkHandlerDescriptor
 */
public class NetworkHandlerDescriptor extends AbstractContextDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.networkHandler"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private DBWHandlerType type;
    private final boolean secured;
    private final String handlerClassName;
    private final String uiClassName;

    public NetworkHandlerDescriptor(
        IConfigurationElement config)
    {
        super(config.getContributor(), config);
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.type = DBWHandlerType.valueOf(config.getAttribute(RegistryConstants.ATTR_TYPE).toUpperCase());
        this.secured = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_CLASS), false);
        this.handlerClassName = config.getAttribute(RegistryConstants.ATTR_HANDLER_CLASS);
        this.uiClassName = config.getAttribute(RegistryConstants.ATTR_UI_CLASS);
    }

    public String getId()
    {
        return id;
    }

    public String getLabel()
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    public DBWHandlerType getType()
    {
        return type;
    }

    public boolean isSecured()
    {
        return secured;
    }

    public <T extends DBWNetworkHandler> T createHandler(Class<T> handlerType)
        throws DBException
    {
        try {
            Class<T> toolClass = getObjectClass(handlerClassName, handlerType);
            return toolClass.newInstance();
        }
        catch (Throwable ex) {
            throw new DBException("Can't create network handler '" + handlerClassName + "'", ex);
        }
    }

    public IObjectPropertyConfigurator createConfigurator()
        throws DBException
    {
        try {
            Class<? extends IObjectPropertyConfigurator> toolClass = getObjectClass(uiClassName, IObjectPropertyConfigurator.class);
            return toolClass.newInstance();
        }
        catch (Throwable ex) {
            throw new DBException("Can't create network handler configurator '" + uiClassName + "'", ex);
        }
    }

}
