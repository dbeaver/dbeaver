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
package org.jkiss.dbeaver.model.net.ssh.config;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;

public class SSHHostConfiguration {
    private final String username;
    private final String hostname;
    private final int port;
    private final SSHAuthConfiguration auth;

    public SSHHostConfiguration(@NotNull String username, @NotNull String hostname, int port, @NotNull SSHAuthConfiguration authConfiguration) {
        this.username = username;
        this.hostname = hostname;
        this.port = port;
        this.auth = authConfiguration;
    }

    public SSHHostConfiguration(@NotNull String username, @NotNull String hostname, @NotNull SSHAuthConfiguration auth) {
        this(username, hostname, SSHConstants.DEFAULT_SSH_PORT, auth);
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    @NotNull
    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    @NotNull
    public SSHAuthConfiguration getAuthConfiguration() {
        return auth;
    }
}
