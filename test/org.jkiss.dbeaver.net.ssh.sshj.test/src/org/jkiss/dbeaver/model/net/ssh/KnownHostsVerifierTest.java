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

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class KnownHostsVerifierTest extends DBeaverUnitTest {
    private static final SSHHostConfiguration HOST = new SSHHostConfiguration(
        "user", "example.test", SSHClient.DEFAULT_PORT, new SSHAuthConfiguration.Password(null, false)
    );

    @TempDir
    Path temporaryDirectory;

    @Test
    public void malformedConfiguredFileReportsPathAndPreservesCause() throws Exception {
        Path knownHosts = Files.writeString(temporaryDirectory.resolve("custom_known_hosts"), "example.test 2048 invalid 3\n");

        DBException error = assertLoadingError(knownHosts, HOST);

        Assertions.assertInstanceOf(NumberFormatException.class, error.getCause());
    }

    @Test
    public void malformedDefaultFileReportsPathAndPreservesCause() throws Exception {
        Path knownHosts = Files.writeString(temporaryDirectory.resolve("known_hosts"), "example.test 2048 invalid 3\n");

        DBException error = assertLoadingError(knownHosts, null);

        Assertions.assertInstanceOf(NumberFormatException.class, error.getCause());
    }

    @Test
    public void malformedSecondaryFileReportsItsOwnPath() throws Exception {
        Path knownHosts = Files.writeString(temporaryDirectory.resolve("known_hosts2"), "example.test 2048 invalid 3\n");

        DBException error = assertLoadingError(knownHosts, null);

        Assertions.assertInstanceOf(NumberFormatException.class, error.getCause());
    }

    @Test
    public void unreadableConfiguredFileReportsPathAndPreservesCause() throws Exception {
        DBException error = assertLoadingError(temporaryDirectory, HOST);

        Assertions.assertInstanceOf(IOException.class, error.getCause());
    }

    @Test
    public void unreadableDefaultFileReportsPathAndPreservesCause() throws Exception {
        Path knownHosts = Files.createDirectory(temporaryDirectory.resolve("known_hosts"));

        DBException error = assertLoadingError(knownHosts, null);

        Assertions.assertInstanceOf(IOException.class, error.getCause());
    }

    @Test
    public void missingFilesAreStillAllowed() throws Exception {
        Path knownHosts = temporaryDirectory.resolve("known_hosts");
        try (SSHClient client = new SSHClient()) {
            Assertions.assertDoesNotThrow(() -> SSHJSessionController.loadKnownHosts(client, knownHosts.toFile(), HOST));
            Assertions.assertDoesNotThrow(() -> SSHJSessionController.loadDefaultKnownHosts(client, temporaryDirectory.toFile()));
        }
        Assertions.assertFalse(Files.exists(knownHosts));
        Assertions.assertFalse(Files.exists(temporaryDirectory.resolve("known_hosts2")));
    }

    @Test
    public void defaultFilesAreLoadedInExistingOrder() throws Exception {
        List<Path> loadedFiles = new ArrayList<>();
        try (SSHClient client = new SSHClient() {
            @Override
            public void loadKnownHosts(@NotNull File location) throws IOException {
                loadedFiles.add(location.toPath());
                super.loadKnownHosts(location);
            }
        }) {
            SSHJSessionController.loadDefaultKnownHosts(client, temporaryDirectory.toFile());
        }

        Assertions.assertEquals(
            List.of(temporaryDirectory.resolve("known_hosts"), temporaryDirectory.resolve("known_hosts2")),
            loadedFiles
        );
    }

    @NotNull
    private DBException assertLoadingError(
        @NotNull Path knownHosts,
        @Nullable SSHHostConfiguration host
    ) throws IOException {
        try (SSHClient client = new SSHClient()) {
            DBException error = Assertions.assertThrows(DBException.class, () -> {
                if (host != null) {
                    SSHJSessionController.loadKnownHosts(client, knownHosts.toFile(), host);
                } else {
                    SSHJSessionController.loadDefaultKnownHosts(client, knownHosts.getParent().toFile());
                }
            });

            Assertions.assertTrue(error.getMessage().contains(knownHosts.toAbsolutePath().toString()));
            Assertions.assertTrue(error.getMessage().contains("readable"));
            Assertions.assertTrue(error.getMessage().contains("valid OpenSSH host keys"));
            return error;
        }
    }

    @Test
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    public void bypassSkipsUnreadableKnownHostsFiles() throws Exception {
        assertKnownHostsSkipped(false, true);
    }

    @Test
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    public void headlessModeSkipsUnreadableKnownHostsFiles() throws Exception {
        assertKnownHostsSkipped(true, false);
    }

    private void assertKnownHostsSkipped(boolean headless, boolean bypass) throws Exception {
        Path sshDirectory = Files.createDirectory(temporaryDirectory.resolve(".ssh"));
        Files.createDirectory(sshDirectory.resolve("known_hosts"));
        Files.writeString(sshDirectory.resolve("known_hosts2"), "example.test 2048 invalid 3\n");
        DBWHandlerConfiguration configuration = Mockito.mock(DBWHandlerConfiguration.class);
        Mockito.when(configuration.getBooleanProperty(SSHConstants.PROP_BYPASS_HOST_VERIFICATION)).thenReturn(bypass);

        String originalUserHome = System.getProperty("user.home");
        try (SSHClient client = Mockito.spy(new SSHClient())) {
            System.setProperty("user.home", temporaryDirectory.toString());

            SSHJSessionController.setupHostKeyVerification(client, configuration, HOST, headless);

            Mockito.verify(client).addHostKeyVerifier(Mockito.isA(PromiscuousVerifier.class));
            Mockito.verify(client, Mockito.times(1)).addHostKeyVerifier(Mockito.any(HostKeyVerifier.class));
            Assertions.assertFalse(client.getTransport().getConfig().isVerifyHostKeyCertificates());
            Mockito.verify(client, Mockito.never()).loadKnownHosts(Mockito.any(File.class));
        } finally {
            if (originalUserHome == null) {
                System.clearProperty("user.home");
            } else {
                System.setProperty("user.home", originalUserHome);
            }
        }
    }
}
