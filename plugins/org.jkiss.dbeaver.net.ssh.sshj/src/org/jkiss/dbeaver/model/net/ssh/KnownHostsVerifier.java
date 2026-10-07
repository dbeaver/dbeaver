/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.function.ThrowableSupplier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.PublicKey;
import java.util.List;

public class KnownHostsVerifier extends OpenSSHKnownHosts {
    private final SSHHostConfiguration actualHostConfiguration;

    public KnownHostsVerifier(@NotNull File khFile, @NotNull SSHHostConfiguration actualHostConfiguration) throws IOException {
        super(khFile);
        this.actualHostConfiguration = actualHostConfiguration;
    }

    @NotNull
    static KnownHostsVerifier create(
        @NotNull File knownHostsFile,
        @NotNull SSHHostConfiguration actualHostConfiguration
    ) throws DBException {
        return load(knownHostsFile, () -> new KnownHostsVerifier(knownHostsFile, actualHostConfiguration));
    }

    @NotNull
    static OpenSSHKnownHosts load(@NotNull File knownHostsFile) throws DBException {
        return load(knownHostsFile, () -> new OpenSSHKnownHosts(knownHostsFile));
    }

    @NotNull
    private static <T extends OpenSSHKnownHosts> T load(
        @NotNull File knownHostsFile,
        @NotNull ThrowableSupplier<T, IOException> loader
    ) throws DBException {
        final T verifier;
        final boolean skippedEntries;
        try {
            verifier = loader.get();
            if (knownHostsFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(knownHostsFile))) {
                    // SSHJ silently drops lines when parsing throws SSHException or SSHRuntimeException.
                    skippedEntries = reader.lines().count() != verifier.entries().size();
                }
            } else {
                skippedEntries = false;
            }
        } catch (IOException | RuntimeException e) {
            throw new DBException(
                "Could not load SSH known hosts file '" + knownHostsFile.getAbsolutePath() +
                    "'. Check that the file is readable and contains valid OpenSSH host keys.",
                e
            );
        }
        // SSHJ may preserve malformed lines instead of throwing a decoding exception.
        if (skippedEntries || verifier.entries().stream().anyMatch(entry ->
            entry instanceof BadHostEntry && !entry.getLine().isBlank() && !entry.getLine().stripLeading().startsWith("#")
        )) {
            throw new DBException(
                "SSH known hosts file '" + knownHostsFile.getAbsolutePath() +
                    "' contains invalid host key entries. Correct or remove the invalid entries and try again."
            );
        }
        return verifier;
    }

    @Override
    public boolean verify(String hostname, int port, PublicKey key) {
        if (hostname.equals(DBConstants.HOST_LOCALHOST) || hostname.equals(DBConstants.HOST_LOCALHOST_IP)) {
            return true;
        } else {
            return super.verify(hostname, port, key);
        }
    }

    @Override
    public List<String> findExistingAlgorithms(String hostname, int port) {
        if (hostname.equals(DBConstants.HOST_LOCALHOST)) {
            return super.findExistingAlgorithms(actualHostConfiguration.hostname(), actualHostConfiguration.port());
        } else {
            return super.findExistingAlgorithms(hostname, port);
        }
    }

    @Override
    protected boolean hostKeyUnverifiableAction(String hostname, PublicKey key) {
        KeyType type = KeyType.fromKey(key);

        boolean isConfirmed = DBWorkbench.getPlatformUI().confirmAction(SSHJUIMessages.verify_connection_confirmation_title,
            NLS.bind(SSHJUIMessages.verify_connection_confirmation_message, new String[]{hostname, type.toString(), SecurityUtils.getFingerprint(key)}), true);

        if (!isConfirmed) {
            return false;
        } else {
            try {
                this.entries().add(new HostEntry(null, hostname, KeyType.fromKey(key), key));
                this.write();
                DBWorkbench.getPlatformUI().showWarningMessageBox(SSHJUIMessages.warning_title,
                    NLS.bind(SSHJUIMessages.known_host_added_warning_message, hostname, type));
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected boolean hostKeyChangedAction(String hostname, PublicKey key) {
        DBWorkbench.getPlatformUI().showWarningMessageBox(
            SSHJUIMessages.warning_title,
            NLS.bind(SSHJUIMessages.host_key_changed_warning_message, new String[]{
                KeyType.fromKey(key).toString(),
                SecurityUtils.getFingerprint(key),
                getFile().getAbsolutePath()
            })
        );
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
