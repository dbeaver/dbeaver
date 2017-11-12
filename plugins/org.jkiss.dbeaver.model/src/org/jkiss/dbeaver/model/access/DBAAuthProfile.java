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
package org.jkiss.dbeaver.model.access;

import java.util.HashMap;
import java.util.Map;

/**
 * Auth profile
 */
public class DBAAuthProfile {
    private Map<String, String> properties = new HashMap<>();

    public DBAAuthProfile() {
    }

    public String getUserName() {
        return properties.get("user");
    }

    public void setUserName(String userName) {
        this.properties.put("user", userName);
    }

    public String getUserPassword() {
        return properties.get("password");
    }

    public void setUserPassword(String userPassword) {
        this.properties.put("password", userPassword);
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public void setProperty(String name, String value) {
        this.properties.put(name, value);
    }

}
