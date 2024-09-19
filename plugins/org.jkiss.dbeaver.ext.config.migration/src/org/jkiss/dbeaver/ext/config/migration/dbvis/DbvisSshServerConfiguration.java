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
package org.jkiss.dbeaver.ext.config.migration.dbvis;

import org.jkiss.dbeaver.model.net.ssh.SSHConstants;

public class DbvisSshServerConfiguration {
    private String id;
    private String name;
    private String description;
    private String isEnabled;
    private String sshHost;
    private String sshPort;
    private String sshUserid;
    private SSHConstants.AuthType authenticationType;
    private String sshPrivateKeyFile;
    private String useSshConfigFile;
    private String sshConfigFile;
    private String sshKnownHostsFile;
    private String sshConnectTimeout;
    private String sshKeepAliveInterval;
    private String sshMaxAuthTries;
    private String savePasswords;
    private String promptPassphrase;
    private String sshJumpServerId;
    private String sshImplementationType;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIsEnabled() {
        return isEnabled;
    }

    public String getSshHost() {
        return sshHost;
    }

    public String getSshPort() {
        return sshPort;
    }

    public String getSshUserid() {
        return sshUserid;
    }

    public SSHConstants.AuthType getAuthenticationType() {
        return authenticationType;
    }

    public String getSshPrivateKeyFile() {
        return sshPrivateKeyFile;
    }

    public String getUseSshConfigFile() {
        return useSshConfigFile;
    }

    public String getSshConfigFile() {
        return sshConfigFile;
    }

    public String getSshKnownHostsFile() {
        return sshKnownHostsFile;
    }

    public String getSshConnectTimeout() {
        return sshConnectTimeout;
    }

    public String getSshKeepAliveInterval() {
        return sshKeepAliveInterval;
    }

    public String getSshMaxAuthTries() {
        return sshMaxAuthTries;
    }

    public String getSavePasswords() {
        return savePasswords;
    }

    public String getPromptPassphrase() {
        return promptPassphrase;
    }

    public String getSshJumpServerId() {
        return sshJumpServerId;
    }

    public String getSshImplementationType() {
        return sshImplementationType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIsEnabled(String isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setSshHost(String sshHost) {
        this.sshHost = sshHost;
    }

    public void setSshPort(String sshPort) {
        this.sshPort = sshPort;
    }

    public void setSshUserid(String sshUserid) {
        this.sshUserid = sshUserid;
    }

    public void setAuthenticationType(String authenticationType) {
        if (authenticationType.equals("PUBLIC_KEY")) {
            this.authenticationType = SSHConstants.AuthType.PUBLIC_KEY;
        } else {
            this.authenticationType = SSHConstants.AuthType.PASSWORD;
        }
    }
 
    public void setSshPrivateKeyFile(String sshPrivateKeyFile) {
        this.sshPrivateKeyFile = sshPrivateKeyFile;
    }

    public void setUseSshConfigFile(String useSshConfigFile) {
        this.useSshConfigFile = useSshConfigFile;
    }

    public void setSshConfigFile(String sshConfigFile) {
        this.sshConfigFile = sshConfigFile;
    }

    public void setSshKnownHostsFile(String sshKnownHostsFile) {
        this.sshKnownHostsFile = sshKnownHostsFile;
    }

    public void setSshConnectTimeout(String sshConnectTimeout) {
        this.sshConnectTimeout = sshConnectTimeout;
    }

    public void setSshKeepAliveInterval(String sshKeepAliveInterval) {
        this.sshKeepAliveInterval = sshKeepAliveInterval;
    }

    public void setSshMaxAuthTries(String sshMaxAuthTries) {
        this.sshMaxAuthTries = sshMaxAuthTries;
    }

    public void setSavePasswords(String savePasswords) {
        this.savePasswords = savePasswords;
    }

    public void setPromptPassphrase(String promptPassphrase) {
        this.promptPassphrase = promptPassphrase;
    }

    public void setSshJumpServerId(String sshJumpServerId) {
        this.sshJumpServerId = sshJumpServerId;
    }

    public void setSshImplementationType(String sshImplementationType) {
        this.sshImplementationType = sshImplementationType;
    }

}
