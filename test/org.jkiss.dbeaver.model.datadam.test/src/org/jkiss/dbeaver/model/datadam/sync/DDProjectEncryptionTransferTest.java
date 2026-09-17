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
package org.jkiss.dbeaver.model.datadam.sync;

import com.dbeaver.datadam.share.api.model.DDSharedProjectConfiguration;
import com.dbeaver.datadam.share.api.model.DDSharedProjectFile;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import com.dbeaver.datadam.share.api.utils.DDFingerprintUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSharedProjectPullResult;
import org.jkiss.dbeaver.model.impl.app.DefaultValueEncryptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.KeyGenerator;

class DDProjectEncryptionTransferTest {
    @TempDir
    Path folder;

    @Test
    void transfersBetweenIndependentProjectKeysAndEncryptionModes() throws Exception {
        Map<String, byte[]> plaintext = Map.of(
            "data-sources.json", "{\"connections\":{}}".getBytes(StandardCharsets.UTF_8),
            "credentials-config.json", "{\"db\":{\"password\":\"synthetic-test-value\"}}".getBytes(StandardCharsets.UTF_8));
        for (boolean sourceEncrypted : new boolean[] { false, true }) {
            for (boolean targetEncrypted : new boolean[] { false, true }) {
                DBPProject source = project(sourceEncrypted);
                DBPProject target = project(targetEncrypted);
                for (var file : DDProjectConfigurationCodec.toLocal(source, plaintext).entrySet()) {
                    Files.write(source.getMetadataFolder(false).resolve(file.getKey()), file.getValue());
                }
                DDShareClient client = Mockito.mock(DDShareClient.class);
                UUID userId = UUID.randomUUID();
                Mockito.when(client.getCurrentShareUserId()).thenReturn(userId);
                AtomicReference<DDSharedProjectPullResult> remote = new AtomicReference<>();
                Mockito.when(client.pushFiles(Mockito.any(), Mockito.anyMap(), Mockito.anyString(),
                    Mockito.eq(DDSharedProjectConfiguration.Format.PORTABLE_JSON))).thenAnswer(call -> {
                        UUID id = call.getArgument(0);
                        Map<String, byte[]> files = call.getArgument(1);
                        for (String name : plaintext.keySet()) {
                            Assertions.assertArrayEquals(plaintext.get(name), files.get(name));
                        }
                        String fingerprint = DDFingerprintUtils.calculateConfigurationFingerprint(id,
                            files.entrySet().stream().map(file -> new DDSharedProjectFile(file.getKey(), "",
                                DDFingerprintUtils.calculateFileFingerprint(id, file.getKey(), file.getValue()))).toList());
                        remote.set(new DDSharedProjectPullResult(fingerprint, files, DDSharedProjectConfiguration.Format.PORTABLE_JSON));
                        return new DDSharedProjectRevision(UUID.randomUUID(), userId, LocalDateTime.now(), fingerprint);
                    });
                Mockito.when(client.pullFiles(Mockito.any())).thenAnswer(call -> remote.get());
                DDProjectShareService service = new DDProjectShareService(client, "account");
                service.upload(Mockito.mock(DBRProgressMonitor.class), source);
                UUID id = service.getRemoteProjectId(source);
                service.download(Mockito.mock(DBRProgressMonitor.class), target, id, true);
                byte[] credentials = Files.readAllBytes(target.getMetadataFolder(false).resolve("credentials-config.json"));
                Assertions.assertArrayEquals(
                    plaintext.get("credentials-config.json"), target.getValueEncryptor().decryptValue(credentials));
                Assertions.assertFalse(Arrays.equals(plaintext.get("credentials-config.json"), credentials));
                byte[] connections = Files.readAllBytes(target.getMetadataFolder(false).resolve("data-sources.json"));
                Assertions.assertArrayEquals(plaintext.get("data-sources.json"), targetEncrypted
                    ? target.getValueEncryptor().decryptValue(connections) : connections);
                // new IVs alone must not cause a local-change conflict
                for (var file : DDProjectConfigurationCodec.toLocal(target, plaintext).entrySet()) {
                    Files.write(target.getMetadataFolder(false).resolve(file.getKey()), file.getValue());
                }
                service.download(Mockito.mock(DBRProgressMonitor.class), target, id, false);
            }
        }
    }

    @Test
    void invalidPortableContentsAreRejectedBeforeWritingPlaintextCredentials() throws Exception {
        DBPProject target = project(true);
        DDShareClient client = Mockito.mock(DDShareClient.class);
        UUID id = UUID.randomUUID();
        Mockito.when(client.getCurrentShareUserId()).thenReturn(UUID.randomUUID());
        Mockito.when(client.pullFiles(id)).thenReturn(new DDSharedProjectPullResult("remote",
            Map.of("credentials-config.json", "not-json".getBytes(StandardCharsets.UTF_8)),
            DDSharedProjectConfiguration.Format.PORTABLE_JSON));
        Assertions.assertThrows(DBException.class, () ->
            new DDProjectShareService(client, "account").download(Mockito.mock(DBRProgressMonitor.class), target, id, true));
        Assertions.assertFalse(Files.exists(target.getMetadataFolder(false).resolve("credentials-config.json")));
        Assertions.assertNull(target.getProjectProperty(DDProjectShareService.BINDING_PROPERTY));
    }

    private DBPProject project(boolean encrypted) throws Exception {
        DBPProject project = Mockito.mock(DBPProject.class);
        Path metadata = Files.createTempDirectory(folder, "project-");
        Mockito.when(project.getMetadataFolder(Mockito.anyBoolean())).thenReturn(metadata);
        Mockito.when(project.getName()).thenReturn("project");
        Mockito.when(project.isEncryptedProject()).thenReturn(encrypted);
        Mockito.when(project.getDataSourceRegistry()).thenReturn(Mockito.mock(DBPDataSourceRegistry.class));
        KeyGenerator keys = KeyGenerator.getInstance("AES");
        keys.init(256);
        Mockito.when(project.getValueEncryptor()).thenReturn(new DefaultValueEncryptor(keys.generateKey()));
        AtomicReference<Object> binding = new AtomicReference<>();
        Mockito.when(project.getProjectProperty(DDProjectShareService.BINDING_PROPERTY)).thenAnswer(call -> binding.get());
        Mockito.doAnswer(call -> { binding.set(call.getArgument(1)); return null; })
            .when(project).setProjectProperty(Mockito.eq(DDProjectShareService.BINDING_PROPERTY), Mockito.any());
        return project;
    }
}
