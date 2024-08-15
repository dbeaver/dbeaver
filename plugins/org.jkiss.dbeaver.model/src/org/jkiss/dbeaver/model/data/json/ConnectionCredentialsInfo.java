/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.data.json;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.GsonUtils;

public class ConnectionCredentialsInfo {

    @Nullable
    private Map<String, Object> authProperties;
    @Nullable
    private List<Map<String, Object>> networkHandlerCredentials;
    @Nullable
    private Boolean saveCredentials;
    @Nullable
    private Boolean sharedCredentials;
    @Nullable
    private String selectedSecretId;

    public ConnectionCredentialsInfo(
        @Nullable Map<String, Object> authProperties,
        @Nullable List<Map<String, Object>> networkHandlerCredentials,
        @Nullable Boolean saveCredentials,
        @Nullable Boolean sharedCredentials,
        @Nullable String selectedSecretId
    ) {
        this.authProperties = authProperties;
        this.networkHandlerCredentials = networkHandlerCredentials;
        this.saveCredentials = saveCredentials;
        this.sharedCredentials = sharedCredentials;
        this.selectedSecretId = selectedSecretId;
    }

    @Nullable
    public String getSelectedSecretId() {
        return this.selectedSecretId;
    }

    public void setSelectedSecretId(@Nullable String selectedSecretId) {
        this.selectedSecretId = selectedSecretId;
    }

    @Nullable
    public Boolean getSharedCredentials() {
        return this.sharedCredentials;
    }

    public void setSharedCredentials(@Nullable Boolean sharedCredentials) {
        this.sharedCredentials = sharedCredentials;
    }

    @Nullable
    public Boolean getSaveCredentials() {
        return this.saveCredentials;
    }

    public void setSaveCredentials(@Nullable Boolean saveCredentials) {
        this.saveCredentials = saveCredentials;
    }

    @NotNull
    public List<Map<String, Object>> getNetworkHandlerCredentials() {
        return this.networkHandlerCredentials != null ? networkHandlerCredentials : Collections.emptyList();
    }

    public void setNetworkHandlerCredentials(@Nullable List<Map<String, Object>> networkHandlerCredentials) {
        this.networkHandlerCredentials = networkHandlerCredentials;
    }

    @NotNull
    public Map<String, Object> getAuthProperties() {
        return this.authProperties != null ? this.authProperties : Collections.emptyMap();
    }

    public void setAuthProperties(@NotNull Map<String, Object> authProperties) {
        this.authProperties = authProperties;
    }

    public String serializeJson() {
        return GsonUtils.gsonBuilder().create().toJson(this, ConnectionCredentialsInfo.class);
    }

    public static ConnectionCredentialsInfo deserializeJson(String json) {
        return GsonUtils.gsonBuilder().create().fromJson(json, ConnectionCredentialsInfo.class);
    }

}
