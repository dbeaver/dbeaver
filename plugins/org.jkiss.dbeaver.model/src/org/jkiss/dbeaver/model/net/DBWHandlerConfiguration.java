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
package org.jkiss.dbeaver.model.net;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.utils.CommonUtils;

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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DBWHandlerConfiguration)) {
            return false;
        }
        DBWHandlerConfiguration source = (DBWHandlerConfiguration)obj;
        return
            CommonUtils.equalObjects(this.descriptor, source.descriptor) &&
            CommonUtils.equalObjects(this.driver, source.driver) &&
            this.enabled == source.enabled &&
            CommonUtils.equalObjects(this.userName, source.userName) &&
            CommonUtils.equalObjects(this.password, source.password) &&
            this.savePassword == source.savePassword &&
            CommonUtils.equalObjects(this.properties, source.properties);
    }
}
