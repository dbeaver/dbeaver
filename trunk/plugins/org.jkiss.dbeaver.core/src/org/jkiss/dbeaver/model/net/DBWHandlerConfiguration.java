/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.model.net;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IObjectPropertyConfiguration;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.registry.NetworkHandlerDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * Network handler configuration
 */
public class DBWHandlerConfiguration implements IObjectPropertyConfiguration {

    private final NetworkHandlerDescriptor descriptor;
    private final DBPDriver driver;
    private boolean enabled;
    private String userName;
    private String password;
    private boolean savePassword;
    private final Map<String, String> properties;

    public DBWHandlerConfiguration(NetworkHandlerDescriptor descriptor, DBPDriver driver)
    {
        this.descriptor = descriptor;
        this.driver = driver;
        this.properties = new HashMap<String, String>();
    }

    public DBWHandlerConfiguration(DBWHandlerConfiguration configuration)
    {
        this.descriptor = configuration.descriptor;
        this.driver = configuration.driver;
        this.enabled = configuration.enabled;
        this.userName = configuration.userName;
        this.password = configuration.password;
        this.savePassword = configuration.savePassword;
        this.properties = new HashMap<String, String>(configuration.properties);
    }

    public <T extends DBWNetworkHandler> T createHandler(Class<T> type) throws DBException
    {
        try {
            return descriptor.createHandler(type);
        } catch (Exception e) {
            throw new DBException("Cannot create tunnel '" + descriptor.getLabel() + "'", e);
        }
    }

    public DBPDriver getDriver()
    {
        return driver;
    }

    public DBWHandlerType getType()
    {
        return descriptor.getType();
    }

    public boolean isSecured()
    {
        return descriptor.isSecured();
    }

    public String getId()
    {
        return descriptor.getId();
    }

    public String getTitle()
    {
        return descriptor.getLabel();
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(@Nullable String password)
    {
        this.password = password;
    }

    public boolean isSavePassword()
    {
        return savePassword;
    }

    public void setSavePassword(boolean savePassword)
    {
        this.savePassword = savePassword;
    }

    @Override
    public Map<String, String> getProperties()
    {
        return properties;
    }

}
