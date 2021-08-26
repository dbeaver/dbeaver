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

import org.eclipse.core.runtime.Assert;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;

import java.io.File;

public class SSHAuthConfiguration {
    private final SSHConstants.AuthType type;
    private final String password;
    private final File key;
    private final boolean savePassword;

    private SSHAuthConfiguration(@NotNull SSHConstants.AuthType type, @Nullable File key, @Nullable String password, boolean savePassword) {
        this.type = type;
        this.password = password;
        this.key = key;
        this.savePassword = savePassword;

        Assert.isLegal(type != SSHConstants.AuthType.PASSWORD || password != null, "Password must not be null");
        Assert.isLegal(type != SSHConstants.AuthType.PUBLIC_KEY || (key != null && key.exists()), "Key must be present");
        Assert.isLegal(type != SSHConstants.AuthType.AGENT || (password == null && key == null && !savePassword), "Password and key may not be present");
    }

    @NotNull
    public static SSHAuthConfiguration usingPassword(@NotNull String password, boolean savePassword) {
        return new SSHAuthConfiguration(SSHConstants.AuthType.PASSWORD, null, password, savePassword);
    }

    @NotNull
    public static SSHAuthConfiguration usingKey(@NotNull File key, @Nullable String passphrase, boolean savePassword) {
        return new SSHAuthConfiguration(SSHConstants.AuthType.PUBLIC_KEY, key, passphrase, savePassword);
    }

    @NotNull
    public static SSHAuthConfiguration usingAgent() {
        return new SSHAuthConfiguration(SSHConstants.AuthType.AGENT, null, null, false);
    }

    @NotNull
    public SSHConstants.AuthType getType() {
        return type;
    }

    @NotNull
    public String getPassword() {
        Assert.isLegal(type.usesPassword());
        return password;
    }

    @NotNull
    public File getKey() {
        Assert.isLegal(type == SSHConstants.AuthType.PUBLIC_KEY);
        return key;
    }

    public boolean isSavePassword() {
        Assert.isLegal(type.usesPassword());
        return savePassword;
    }
}
