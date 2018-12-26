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
package org.jkiss.dbeaver.registry.network;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.net.DBWHandlerDescriptor;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * NetworkHandlerDescriptor
 */
public class NetworkHandlerDescriptor extends AbstractContextDescriptor implements DBWHandlerDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.networkHandler"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private DBWHandlerType type;
    private final boolean secured;
    private final ObjectType handlerType;
    private final int order;

    NetworkHandlerDescriptor(
        IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.type = DBWHandlerType.valueOf(config.getAttribute(RegistryConstants.ATTR_TYPE).toUpperCase(Locale.ENGLISH));
        this.secured = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_SECURED), false);
        this.handlerType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_HANDLER_CLASS));
        this.order = CommonUtils.toInt(config.getAttribute(RegistryConstants.ATTR_ORDER), 1);
    }

    @NotNull
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

    public int getOrder() {
        return order;
    }

    public boolean matches(DBPDataSourceProvider provider)
    {
        return appliesTo(provider);
    }

    public ObjectType getHandlerType() {
        return handlerType;
    }

    public <T extends DBWNetworkHandler> T createHandler(Class<T> impl)
        throws DBException
    {
        return handlerType.createInstance(impl);
    }

}
