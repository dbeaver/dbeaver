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
package org.jkiss.dbeaver.model.net;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Network handler configuration
 */
public class DBWHandlerConfiguration {

    public static final String PROP_HOST = "host";
    public static final String PROP_PORT = "port";

    @NotNull
    private final DBWHandlerDescriptor descriptor;
    private DBPDataSourceContainer dataSource;
    private boolean enabled;
    private String userName;
    private String password;
    private boolean savePassword = true;
    private final Map<String, Object> properties;
    private final Map<String, String> secureProperties;

    public DBWHandlerConfiguration(@NotNull DBWHandlerDescriptor descriptor, DBPDataSourceContainer dataSource) {
        this.descriptor = descriptor;
        this.dataSource = dataSource;
        this.properties = new HashMap<>();
        this.secureProperties = new HashMap<>();
    }

    public DBWHandlerConfiguration(@NotNull DBWHandlerConfiguration configuration) {
        this.descriptor = configuration.descriptor;
        this.dataSource = configuration.dataSource;
        this.enabled = configuration.enabled;
        this.userName = configuration.userName;
        this.password = configuration.password;
        this.savePassword = configuration.savePassword;
        this.properties = new HashMap<>(configuration.properties);
        this.secureProperties = new HashMap<>(configuration.secureProperties);
    }

    @NotNull
    public DBWHandlerDescriptor getHandlerDescriptor() {
        return descriptor;
    }

    public <T extends DBWNetworkHandler> T createHandler(Class<T> type) throws DBException {
        try {
            return descriptor.createHandler(type);
        } catch (Exception e) {
            throw new DBException("Cannot create tunnel '" + descriptor.getLabel() + "'", e);
        }
    }

    public DBPDriver getDriver() {
        return dataSource == null ? null : dataSource.getDriver();
    }

    public DBPDataSourceContainer getDataSource() {
        return dataSource;
    }

    public void setDataSource(DBPDataSourceContainer dataSource) {
        this.dataSource = dataSource;
    }

    public DBWHandlerType getType() {
        return descriptor.getType();
    }

    public boolean isSecured() {
        return descriptor.isSecured();
    }

    @NotNull
    public String getId() {
        return descriptor.getId();
    }

    public String getTitle() {
        return descriptor.getLabel();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    public boolean isSavePassword() {
        return savePassword;
    }

    public void setSavePassword(boolean savePassword) {
        this.savePassword = savePassword;
    }

    @Nullable
    public Object getProperty(@NotNull String name) {
        return this.properties.get(name);
    }

    @Nullable
    public String getStringProperty(@NotNull String name) {
        return CommonUtils.toString(this.properties.get(name), null);
    }

    public int getIntProperty(@NotNull String name) {
        return CommonUtils.toInt(this.properties.get(name));
    }

    public boolean getBooleanProperty(@NotNull String name) {
        return this.getBooleanProperty(name, false);
    }

    public boolean getBooleanProperty(@NotNull String name, boolean defValue) {
        return CommonUtils.getBoolean(this.properties.get(name), defValue);
    }

    public void setProperty(@NotNull String name, @Nullable Object value) {
        if (value == null) {
            this.properties.remove(name);
        } else {
            this.properties.put(name, value);
        }
    }

    @NotNull
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(@NotNull Map<String, Object> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    @Nullable
    public String getSecureProperty(@NotNull String name) {
        return secureProperties.get(name);
    }

    @NotNull
    public Map<String, String> getSecureProperties() {
        return secureProperties;
    }

    public void setSecureProperty(@NotNull String name, @Nullable String value) {
        secureProperties.put(name, value);
    }

    public void setSecureProperties(@NotNull Map<String, String> secureProperties) {
        this.secureProperties.clear();
        this.secureProperties.putAll(secureProperties);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DBWHandlerConfiguration)) {
            return false;
        }
        DBWHandlerConfiguration source = (DBWHandlerConfiguration) obj;
        return
            CommonUtils.equalObjects(this.descriptor, source.descriptor) &&
                CommonUtils.equalObjects(this.dataSource, source.dataSource) &&
                this.enabled == source.enabled &&
                CommonUtils.equalObjects(this.userName, source.userName) &&
                CommonUtils.equalObjects(this.password, source.password) &&
                this.savePassword == source.savePassword &&
                CommonUtils.equalObjects(this.properties, source.properties) &&
                CommonUtils.equalObjects(this.secureProperties, source.secureProperties);
    }

    public void resolveDynamicVariables(IVariableResolver variableResolver) {
        userName = GeneralUtils.replaceVariables(userName, variableResolver);
        password = GeneralUtils.replaceVariables(password, variableResolver);
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            if (prop.getValue() instanceof String && CommonUtils.isNotEmpty((String) prop.getValue())) {
                prop.setValue(GeneralUtils.replaceVariables((String) prop.getValue(), variableResolver));
            }
        }
        for (Map.Entry<String, String> prop : secureProperties.entrySet()) {
            if (CommonUtils.isNotEmpty(prop.getValue())) {
                prop.setValue(GeneralUtils.replaceVariables(prop.getValue(), variableResolver));
            }
        }
    }

    public boolean hasValuableInfo() {
        return !CommonUtils.isEmpty(userName) ||
            !CommonUtils.isEmpty(password) ||
            !CommonUtils.isEmpty(properties) ||
            !CommonUtils.isEmpty(secureProperties);
    }

    @Override
    public String toString() {
        return descriptor.toString();
    }

}
