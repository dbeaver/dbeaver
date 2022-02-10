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
package org.jkiss.dbeaver.model.net.ssh;

import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;

public class KnownHostsVerifier extends OpenSSHKnownHosts {

    private DBPPlatformUI platformUI;

    public KnownHostsVerifier(@NotNull File khFile, @NotNull DBPPlatformUI platformUI) throws IOException {
        super(khFile);
        this.platformUI = platformUI;
    }

    @Override
    protected boolean hostKeyUnverifiableAction(String hostname, PublicKey key) {
        KeyType type = KeyType.fromKey(key);

        String confirmationMessage = new StringBuilder()
                .append("The authenticity of host can't be established.")
                .append("host: '").append(hostname).append("'\n")
                .append(type).append("key fingerprint: ").append(SecurityUtils.getFingerprint(key)).append("\n")
                .append("Are you sure you want to continue connecting?").toString();

        boolean isConfirmed = platformUI.confirmAction("Connection confirmation", confirmationMessage);

        if(isConfirmed) {
            try {
                this.entries().add(new HostEntry((Marker)null, hostname, KeyType.fromKey(key), key));
                this.write();
                String warnMessage = new StringBuilder()
                        .append("'").append(hostname).append("' (").append(type).append(") ")
                        .append("permanently added to the list of known hosts.\n").toString();
                platformUI.showWarningMessageBox("Warning",  warnMessage);
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return false;
        }
    }

    @Override
    protected boolean hostKeyChangedAction(String hostname, PublicKey key) {
        KeyType type = KeyType.fromKey(key);
        String fp = SecurityUtils.getFingerprint(key);
        String path = this.getFile().getAbsolutePath();
        String warnMessage = new StringBuilder()
                .append("Remote host identification has changed. It could be man-in-the-middle attack or the host key has just been changed \n")
                .append("The fingerprint for the ").append(type).append("key sent by the remote host is ").append(fp).append("\n")
                .append("Please contact your system administrator or add correct host key in ").append(path).append("to get rid of this message.")
                .append("\n").toString();
        platformUI.showWarningMessageBox("Warning",warnMessage);
        return false;
    }

    @Override
    public void write(KnownHostEntry entry) throws IOException {
        this.khFile.getParentFile().mkdirs();
        super.write(entry);
        SSHUtils.forcePlatformReloadKnownHostsPreferences();
    }

    @Override
    public void write() throws IOException {
        this.khFile.getParentFile().mkdirs();
        super.write();
        SSHUtils.forcePlatformReloadKnownHostsPreferences();
    }
}
