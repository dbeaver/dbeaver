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
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class KnownHostsVerifierTest extends DBeaverUnitTest {
    private static final SSHHostConfiguration HOST = new SSHHostConfiguration(
        "user", "example.test", SSHClient.DEFAULT_PORT, new SSHAuthConfiguration.Password(null, false)
    );

    @TempDir
    Path temporaryDirectory;

    @Test
    public void malformedBase64HasActionableError() throws Exception {
        assertInvalidEntry("example.test ssh-rsa not.base64\n");
    }

    @Test
    public void malformedEntryHasActionableError() throws Exception {
        assertInvalidEntry("example.test ssh-rsa\n");
    }

    @Test
    public void malformedHashedHostnameIsNotSilentlyIgnored() throws Exception {
        PublicKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        String entry = new OpenSSHKnownHosts.HostEntry(null, HOST.hostname(), KeyType.RSA, key).getLine();

        assertInvalidEntry("|1|invalid" + entry.substring(entry.indexOf(' ')) + "\n");
    }

    @Test
    public void malformedLegacyEntryPreservesCause() throws Exception {
        Path knownHosts = writeKnownHosts("example.test 2048 invalid 3\n");

        DBException error = Assertions.assertThrows(DBException.class, () -> load(knownHosts));

        Assertions.assertTrue(error.getMessage().contains(knownHosts.toAbsolutePath().toString()));
        Assertions.assertTrue(error.getMessage().contains("valid OpenSSH host keys"));
        Assertions.assertInstanceOf(NumberFormatException.class, error.getCause());
    }

    @Test
    public void unreadableFilePreservesCause() {
        DBException error = Assertions.assertThrows(DBException.class, () -> load(temporaryDirectory));

        Assertions.assertTrue(error.getMessage().contains(temporaryDirectory.toAbsolutePath().toString()));
        Assertions.assertTrue(error.getMessage().contains("readable"));
        Assertions.assertInstanceOf(IOException.class, error.getCause());
    }

    @Test
    public void missingFileIsAllowedForFirstConnection() throws Exception {
        Path knownHosts = temporaryDirectory.resolve("known_hosts");

        Assertions.assertTrue(load(knownHosts).entries().isEmpty());
        Assertions.assertFalse(Files.exists(knownHosts));
    }

    @Test
    public void emptyFileIsAllowed() throws Exception {
        Assertions.assertTrue(load(writeKnownHosts("")).entries().isEmpty());
    }

    @Test
    public void commentsAndBlankLinesAreAllowed() throws Exception {
        KnownHostsVerifier verifier = load(writeKnownHosts("# SSH host keys\n\n"));

        Assertions.assertTrue(verifier.entries().stream().allMatch(entry -> entry instanceof OpenSSHKnownHosts.CommentEntry));
    }

    @Test
    public void whitespaceOnlyLinesAreAllowed() throws Exception {
        String content = " \t\n\t  \n";
        Path knownHosts = writeKnownHosts(content);

        Assertions.assertDoesNotThrow(() -> load(knownHosts));
        Assertions.assertEquals(content, Files.readString(knownHosts));
    }

    @Test
    public void indentedCommentsAreAllowedAlongsideValidKeys() throws Exception {
        PublicKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        String entry = new OpenSSHKnownHosts.HostEntry(null, HOST.hostname(), KeyType.RSA, key).getLine();
        String content = "  # SSH host keys\n \t\n" + entry + "\n\t# Another comment\n";
        Path knownHosts = writeKnownHosts(content);

        Assertions.assertTrue(load(knownHosts).verify(HOST.hostname(), HOST.port(), key));
        Assertions.assertEquals(content, Files.readString(knownHosts));
    }

    @Test
    public void validHostKeyIsVerified() throws Exception {
        PublicKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        String entry = new OpenSSHKnownHosts.HostEntry(null, HOST.hostname(), KeyType.RSA, key).getLine();
        KnownHostsVerifier verifier = load(writeKnownHosts(entry + "\n"));

        Assertions.assertEquals(1, verifier.entries().size());
        Assertions.assertTrue(verifier.verify(HOST.hostname(), HOST.port(), key));
    }

    @Test
    public void invalidEntryIsNotIgnoredAlongsideValidKeys() throws Exception {
        PublicKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        String entry = new OpenSSHKnownHosts.HostEntry(null, HOST.hostname(), KeyType.RSA, key).getLine();

        assertInvalidEntry(entry + "\nother.test ssh-rsa not.base64\n");
    }

    @Test
    public void secondaryHostKeysAreLoadedBeforeInteractiveVerification() throws Exception {
        PublicKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        String entry = new OpenSSHKnownHosts.HostEntry(null, HOST.hostname(), KeyType.RSA, key).getLine();
        Path secondary = Files.writeString(temporaryDirectory.resolve("known_hosts2"), entry + "\n");
        Path primary = writeKnownHosts("");
        RecordingSSHClient client = new RecordingSSHClient();

        SSHJSessionController.loadKnownHosts(client, primary.toFile(), temporaryDirectory.toFile(), HOST);

        Assertions.assertEquals(2, client.verifiers.size());
        Assertions.assertTrue(client.verifiers.get(0).verify(HOST.hostname(), HOST.port(), key));
        Assertions.assertFalse(client.verifiers.get(0) instanceof KnownHostsVerifier);
        Assertions.assertInstanceOf(KnownHostsVerifier.class, client.verifiers.get(1));
        Assertions.assertEquals("", Files.readString(primary));
        Assertions.assertEquals(entry + "\n", Files.readString(secondary));
    }

    @Test
    public void missingSecondaryFileDoesNotPreventLoadingPrimaryKeys() throws Exception {
        PublicKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        String entry = new OpenSSHKnownHosts.HostEntry(null, HOST.hostname(), KeyType.RSA, key).getLine();
        Path primary = writeKnownHosts(entry + "\n");
        RecordingSSHClient client = new RecordingSSHClient();

        SSHJSessionController.loadKnownHosts(client, primary.toFile(), temporaryDirectory.toFile(), HOST);

        Assertions.assertTrue(client.verifiers.get(1).verify(HOST.hostname(), HOST.port(), key));
        Assertions.assertFalse(Files.exists(temporaryDirectory.resolve("known_hosts2")));
    }

    @Test
    public void malformedSecondaryFileReportsItsOwnPath() throws Exception {
        Path primary = writeKnownHosts("");
        Path secondary = Files.writeString(temporaryDirectory.resolve("known_hosts2"), "example.test ssh-rsa not.base64\n");
        RecordingSSHClient client = new RecordingSSHClient();

        DBException error = Assertions.assertThrows(DBException.class, () ->
            SSHJSessionController.loadKnownHosts(client, primary.toFile(), temporaryDirectory.toFile(), HOST)
        );

        Assertions.assertTrue(error.getMessage().contains(secondary.toAbsolutePath().toString()));
        Assertions.assertTrue(error.getMessage().contains("Correct or remove the invalid entries"));
        Assertions.assertTrue(client.verifiers.isEmpty());
    }

    private void assertInvalidEntry(@NotNull String content) throws Exception {
        Path knownHosts = writeKnownHosts(content);

        DBException error = Assertions.assertThrows(DBException.class, () -> load(knownHosts));

        Assertions.assertTrue(error.getMessage().contains(knownHosts.toAbsolutePath().toString()));
        Assertions.assertTrue(error.getMessage().contains("Correct or remove the invalid entries"));
        Assertions.assertEquals(content, Files.readString(knownHosts));
    }

    @NotNull
    private Path writeKnownHosts(@NotNull String content) throws IOException {
        return Files.writeString(temporaryDirectory.resolve("known_hosts"), content);
    }

    @NotNull
    private KnownHostsVerifier load(@NotNull Path knownHosts) throws DBException {
        return KnownHostsVerifier.create(knownHosts.toFile(), HOST);
    }

    private static class RecordingSSHClient extends SSHClient {
        private final List<HostKeyVerifier> verifiers = new ArrayList<>();

        @Override
        public void addHostKeyVerifier(@NotNull HostKeyVerifier verifier) {
            verifiers.add(verifier);
        }
    }
}
