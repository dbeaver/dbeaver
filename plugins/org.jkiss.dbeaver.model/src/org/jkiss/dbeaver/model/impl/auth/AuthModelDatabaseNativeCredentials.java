/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.impl.auth;

import org.jkiss.dbeaver.model.auth.DBAAuthCredentials;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * Database native auth credentials.
 */
public class AuthModelDatabaseNativeCredentials implements DBAAuthCredentials {

    private String userName;
    private String userPassword;

    @Property(order = 1)
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Property(order = 2, password = true)
    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public String[] getMissingProperties() {
        return null;
    }
}
