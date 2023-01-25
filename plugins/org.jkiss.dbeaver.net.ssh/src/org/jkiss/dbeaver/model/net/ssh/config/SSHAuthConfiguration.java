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
package org.jkiss.dbeaver.model.net.ssh.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;

import java.nio.file.Path;

public class SSHAuthConfiguration {
    private final SSHConstants.AuthType type;
    private final String password;
    private final Path keyFile;
    private final String keyValue;
    private final boolean savePassword;

    private SSHAuthConfiguration(@NotNull SSHConstants.AuthType type, @Nullable String password, boolean savePassword) {
        this.type = type;
        this.password = password;
        this.keyFile = null;
        this.keyValue = null;
        this.savePassword = savePassword;
    }

    private SSHAuthConfiguration(@NotNull SSHConstants.AuthType type, @Nullable Path keyFile, @Nullable String password, boolean savePassword) {
        this.type = type;
        this.password = password;
        this.keyFile = keyFile;
        this.keyValue = null;
        this.savePassword = savePassword;
    }

    private SSHAuthConfiguration(@NotNull SSHConstants.AuthType type, @NotNull String keyValue, @Nullable String password, boolean savePassword) {
        this.type = type;
        this.password = password;
        this.keyFile = null;
        this.keyValue = keyValue;
        this.savePassword = savePassword;
    }

    @NotNull
    public static SSHAuthConfiguration usingPassword(@NotNull String password, boolean savePassword) {
        return new SSHAuthConfiguration(SSHConstants.AuthType.PASSWORD, password, savePassword);
    }

    @NotNull
    public static SSHAuthConfiguration usingKey(@NotNull Path key, @Nullable String passphrase, boolean savePassword) {
        return new SSHAuthConfiguration(SSHConstants.AuthType.PUBLIC_KEY, key, passphrase, savePassword);
    }

    @NotNull
    public static SSHAuthConfiguration usingKey(@NotNull String keyValue, @Nullable String passphrase, boolean savePassword) {
        return new SSHAuthConfiguration(SSHConstants.AuthType.PUBLIC_KEY, keyValue, passphrase, savePassword);
    }

    @NotNull
    public static SSHAuthConfiguration usingAgent() {
        return new SSHAuthConfiguration(SSHConstants.AuthType.AGENT, null, false);
    }

    @NotNull
    public SSHConstants.AuthType getType() {
        return type;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    @Nullable
    public Path getKeyFile() {
        return keyFile;
    }

    @Nullable
    public String getKeyValue() {
        return keyValue;
    }

    public boolean isSavePassword() {
        return savePassword;
    }

}
