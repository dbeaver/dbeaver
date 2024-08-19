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
package io.cloudbeaver.model;


import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.utils.CommonUtils;

public class WebNetworkHandlerConfigInput {

    private final Map<String, Object> cfg;

    public WebNetworkHandlerConfigInput() {
        this.cfg = new HashMap<>();
    }

    public WebNetworkHandlerConfigInput(Map<String, Object> cfg) {
        this.cfg = cfg;
    }

    public DBWHandlerType getType() {
        return CommonUtils.valueOf(DBWHandlerType.class, JSONUtils.getString(cfg, "type"), null);
    }

    public void setType(DBWHandlerType type) {
        cfg.put("type", type.name());
    }

    public String getId() {
        return JSONUtils.getString(cfg, "id");
    }

    public void setId(String id) {
        cfg.put("id", id);
    }

    public Boolean isEnabled() {
        if (cfg.containsKey("enabled")) {
            return JSONUtils.getBoolean(cfg, "enabled");
        } else {
            return null;
        }
    }

    public void setEnabled(Boolean enabled) {
        cfg.put("enabled", enabled);
    }

    public String getAuthType() {
        return JSONUtils.getString(cfg, "authType");
    }

    public void setAuthType(String authType) { cfg.put("authType", authType); }

    public String getUserName() {
        return JSONUtils.getString(cfg, "userName");
    }

    public void setUserName(String userName) { cfg.put("userName", userName); }

    public String getPassword() {
        return JSONUtils.getString(cfg, "password");
    }

    public void setPassword(String password) { cfg.put("password", password); }

    @Deprecated // use secured properties
    public String getKey() {
        return JSONUtils.getString(cfg, "key");
    }

    public void setKey(String key) { cfg.put("key", key); }

    public Boolean isSavePassword() {
        if (cfg.containsKey("savePassword")) {
            return JSONUtils.getBoolean(cfg, "savePassword");
        } else {
            return null;
        }
    }

    public void setSavePassword(Boolean savePassword) { cfg.put("savePassword", savePassword); }

    public Map<String, Object> getProperties() {
        return JSONUtils.getObjectOrNull(cfg, "properties");
    }

    public void setProperties(Map<String, Object> properties) { cfg.put("properties", properties); }

    public Map<String, String> getSecureProperties() {
        var secureProperties = JSONUtils.getObjectOrNull(cfg, "secureProperties");
        if (secureProperties == null) {
            return null;
        }
        var result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Object> property : secureProperties.entrySet()) {
            result.put(property.getKey(), CommonUtils.toString(property.getValue()));
        }
        return result;
    }

    public void setSecureProperties(Map<String, String> secureProperties) {
        cfg.put("secureProperties", secureProperties);
    }

    public Map<String, Object> toMap() {
        return new HashMap<>(cfg);
    }
}

