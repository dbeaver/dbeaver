/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License; Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing; software
 * distributed under the License is distributed on an "AS IS" BASIS;
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND; either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.qm;

public class QMSessionInfo {
    private final String userName;
    private final String userDomain;

    private final String userIp;

    public QMSessionInfo(String userName, String userDomain, String userIp) {
        this.userName = userName;
        this.userDomain = userDomain;
        this.userIp = userIp;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public String getUserIp() {
        return userIp;
    }
}
