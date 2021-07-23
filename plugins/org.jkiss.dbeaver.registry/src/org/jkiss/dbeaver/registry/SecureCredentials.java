/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthProfile;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class SecureCredentials {

    @Nullable
    private String userName;
    @Nullable
    private String userPassword;
    @Nullable
    private Map<String, String> properties;

    public SecureCredentials() {
    }

    public SecureCredentials(@NotNull DBPDataSourceContainer dataSource) {
        this.userName = dataSource.getConnectionConfiguration().getUserName();
        this.userPassword = dataSource.isSavePassword() ? dataSource.getConnectionConfiguration().getUserPassword() : null;
    }

    public SecureCredentials(@NotNull DBAAuthProfile profile) {
        this.userName = profile.getUserName();
        this.userPassword = profile.getUserPassword();
        this.properties = profile.getProperties();
    }

    public SecureCredentials(@NotNull DBWHandlerConfiguration handlerConfiguration) {
        this.userName = handlerConfiguration.getUserName();
        this.userPassword = handlerConfiguration.isSavePassword() ? handlerConfiguration.getPassword() : null;
    }

    @Nullable
    public String getUserName() {
        return userName;
    }

    public void setUserName(@Nullable String userName) {
        this.userName = userName;
    }

    @Nullable
    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(@Nullable String userPassword) {
        this.userPassword = userPassword;
    }

    @Nullable
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(@NotNull Map<String, String> properties) {
        if (this.properties != null) {
            this.properties.clear();
            this.properties.putAll(properties);
        } else {
            this.properties = new HashMap<>(properties);
        }
    }

    public void setSecureProp(String key, String value) {
        if (this.properties == null) {
            this.properties = new LinkedHashMap<>();
        }
        this.properties.put(key, value);
    }
}
