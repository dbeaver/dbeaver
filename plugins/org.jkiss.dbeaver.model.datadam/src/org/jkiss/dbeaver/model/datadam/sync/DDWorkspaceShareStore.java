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

import com.dbeaver.datadam.share.api.exception.DDShareException;
import com.dbeaver.datadam.share.api.model.DDSharedWorkspace;
import com.dbeaver.datadam.share.api.service.DDSharedWorkspaceService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class DDWorkspaceShareStore {
    private static final byte[] KEY_CONTEXT = "dbeaver-workspace-key-v1".getBytes(StandardCharsets.UTF_8);
    private final DDSharedWorkspaceService service;
    private final SecretKey accountKey;

    public DDWorkspaceShareStore(@NotNull DDSharedWorkspaceService service, @NotNull SecretKey accountKey) {
        this.service = service;
        this.accountKey = accountKey;
    }

    @NotNull
    public DDSharedWorkspace createWorkspace(@NotNull String name) throws DBException, DDShareException {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        try {
            String encryptedKey = Base64.getEncoder().encodeToString(DDCrypto.encrypt(accountKey, key, KEY_CONTEXT));
            return service.createWorkspace(name, encryptedKey);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    @Nullable
    public byte[] readFile(@NotNull DDSharedWorkspace workspace, @NotNull String fileName) throws DBException, DDShareException {
        String ciphertext = service.readConfigFile(workspace.id(), fileName);
        if (ciphertext == null) {
            return null;
        }
        try {
            return DDCrypto.decrypt(workspaceKey(workspace), Base64.getDecoder().decode(ciphertext),
                DDShareClient.aad(workspace.id().toString(), fileName));
        } catch (IllegalArgumentException e) {
            throw new DBException("Invalid workspace ciphertext", e);
        }
    }

    public void writeFile(@NotNull DDSharedWorkspace workspace, @NotNull String fileName, @NotNull byte[] contents)
        throws DBException, DDShareException {
        String ciphertext = Base64.getEncoder().encodeToString(DDCrypto.encrypt(workspaceKey(workspace), contents,
            DDShareClient.aad(workspace.id().toString(), fileName)));
        if (!service.writeConfigFile(workspace.id(), fileName, ciphertext)) {
            throw new DBException("Workspace file was not saved");
        }
    }

    @NotNull
    private SecretKey workspaceKey(@NotNull DDSharedWorkspace workspace) throws DBException {
        byte[] key;
        try {
            key = DDCrypto.decrypt(accountKey, Base64.getDecoder().decode(workspace.encryptedKey()), KEY_CONTEXT);
        } catch (IllegalArgumentException e) {
            throw new DBException("Invalid workspace key", e);
        }
        try {
            if (key.length != 32) {
                throw new DBException("Invalid workspace key size");
            }
            return new SecretKeySpec(key, "AES");
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }
}
