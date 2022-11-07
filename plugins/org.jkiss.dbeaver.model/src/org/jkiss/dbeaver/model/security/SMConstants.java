/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.security;

public interface SMConstants {
    String SESSION_PARAM_LAST_REMOTE_ADDRESS = "lastRemoteAddr";
    String SESSION_PARAM_LAST_REMOTE_USER_AGENT = "lastRemoteUserAgent";
    String SESSION_PARAM_TRUSTED_USER_TEAMS = "trustedUserTeams";

    String SUBJECT_PERMISSION_SCOPE = "subject";
    String PROJECT_PERMISSION_SCOPE = "project";

    String DATA_SOURCE_ACCESS_PERMISSION = "access";

    String USER_AUTH_ROLE_PARAM = "authRole";
}
