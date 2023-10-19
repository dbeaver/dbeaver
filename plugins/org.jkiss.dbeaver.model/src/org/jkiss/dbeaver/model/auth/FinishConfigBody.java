/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.auth;

import java.util.List;

public class FinishConfigBody {

    private final String user;
    private final String password;
    private final List<AuthInfo> authInfos;

    public FinishConfigBody(String user, String password, List<AuthInfo> authInfos) {
        this.user = user;
        this.password = password;
        this.authInfos = authInfos;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public List<AuthInfo> getAuthInfos() {
        return authInfos;
    }
}
