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

import com.dbeaver.datadam.share.api.model.DDSharedWorkspace;
import com.dbeaver.datadam.share.api.service.DDSharedWorkspaceService;
import org.jkiss.dbeaver.DBException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.crypto.KeyGenerator;

class DDWorkspaceShareStoreTest {
    @Test
    void workspaceKeyRoundTripAndAuthenticatedFileIdentity() throws Exception {
        DDSharedWorkspaceService service = Mockito.mock(DDSharedWorkspaceService.class);
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        DDWorkspaceShareStore store = new DDWorkspaceShareStore(service, generator.generateKey());
        Mockito.when(service.createWorkspace(Mockito.anyString(), Mockito.anyString())).thenAnswer(call ->
            new DDSharedWorkspace(UUID.randomUUID(), call.getArgument(0), call.getArgument(1),
                OffsetDateTime.now(), OffsetDateTime.now()));
        DDSharedWorkspace first = store.createWorkspace("first");
        DDSharedWorkspace second = store.createWorkspace("second");
        Assertions.assertNotEquals(first.encryptedKey(), second.encryptedKey());
        Mockito.when(service.writeConfigFile(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        byte[] plaintext = "workspace settings".getBytes(StandardCharsets.UTF_8);
        store.writeFile(first, "settings.json", plaintext);
        ArgumentCaptor<String> ciphertext = ArgumentCaptor.forClass(String.class);
        Mockito.verify(service).writeConfigFile(Mockito.eq(first.id()), Mockito.eq("settings.json"), ciphertext.capture());
        Mockito.when(service.readConfigFile(Mockito.any(), Mockito.anyString())).thenReturn(ciphertext.getValue());
        Assertions.assertArrayEquals(plaintext, store.readFile(first, "settings.json"));
        Assertions.assertThrows(DBException.class, () -> store.readFile(first, "renamed.json"));
        Assertions.assertThrows(DBException.class, () -> store.readFile(second, "settings.json"));
    }
}
