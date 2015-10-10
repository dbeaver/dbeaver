/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.net;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPDriver;

import java.util.HashMap;
import java.util.Map;

/**
 * Network handler configuration
 */
public class DBWHandlerConfiguration {

    @NotNull
    private final DBWHandlerDescriptor descriptor;
    private final DBPDriver driver;
    private boolean enabled;
    private String userName;
    private String password;
    private boolean savePassword;
    private final Map<String, String> properties;

    public DBWHandlerConfiguration(@NotNull DBWHandlerDescriptor descriptor, DBPDriver driver)
    {
        this.descriptor = descriptor;
        this.driver = driver;
        this.properties = new HashMap<>();
    }

    public DBWHandlerConfiguration(@NotNull DBWHandlerConfiguration configuration)
    {
        this.descriptor = configuration.descriptor;
        this.driver = configuration.driver;
        this.enabled = configuration.enabled;
        this.userName = configuration.userName;
        this.password = configuration.password;
        this.savePassword = configuration.savePassword;
        this.properties = new HashMap<>(configuration.properties);
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

    @NotNull
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

    public Map<String, String> getProperties()
    {
        return properties;
    }

}
