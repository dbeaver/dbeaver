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
package org.jkiss.dbeaver.model.tracking.sync.core;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.tracking.auth.DDCrypto;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;

/**
 * Encrypted view over a transport: resources are packed and encrypted here,
 * the transport below sees opaque bytes only.
 */
public class DDSyncStore {

    private final DDSyncTransport transport;
    private final DDSyncCredentials credentials;

    private SecretKey dataKey;

    public DDSyncStore(@NotNull String url, @NotNull DDSyncCredentials credentials) {
        this(new DDRestTransport(url, credentials), credentials);
    }

    public DDSyncStore(@NotNull DDSyncTransport transport, @NotNull DDSyncCredentials credentials) {
        this.transport = transport;
        this.credentials = credentials;
    }

    @NotNull
    public List<DDContainer> listContainers() throws DBException {
        return transport.listContainers();
    }

    @NotNull
    public DDContainer createContainer(@NotNull String label) throws DBException {
        return transport.createContainer(label);
    }

    @NotNull
    public List<DDSyncEntry> load(@NotNull String containerId) throws DBException {
        SecretKey key = getDataKey();
        List<DDSyncEntry> entries = new ArrayList<>();
        for (DDRawEntry raw : transport.load(containerId)) {
            DDSyncEnvelope envelope = JSONUtils.GSON.fromJson(
                new String(DDCrypto.decrypt(key, raw.value()), StandardCharsets.UTF_8),
                DDSyncEnvelope.class);
            entries.add(new DDSyncEntry(
                raw.key(),
                envelope.label(),
                null,
                decode(envelope.resources())));
        }
        return entries;
    }

    public void save(@NotNull String containerId, @NotNull DDSyncEntry entry) throws DBException {
        byte[] content = JSONUtils.GSON
            .toJson(new DDSyncEnvelope(entry.label(), encode(entry.resources())))
            .getBytes(StandardCharsets.UTF_8);
        transport.save(containerId, entry.key(), DDCrypto.encrypt(getDataKey(), content));
    }

    @NotNull
    private static Map<String, String> encode(@NotNull Map<String, byte[]> resources) {
        Map<String, String> encoded = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> resource : resources.entrySet()) {
            encoded.put(resource.getKey(), Base64.getEncoder().encodeToString(resource.getValue()));
        }
        return encoded;
    }

    @NotNull
    private static Map<String, byte[]> decode(@NotNull Map<String, String> resources) {
        Map<String, byte[]> decoded = new LinkedHashMap<>();
        for (Map.Entry<String, String> resource : resources.entrySet()) {
            decoded.put(resource.getKey(), Base64.getDecoder().decode(resource.getValue()));
        }
        return decoded;
    }

    @NotNull
    private SecretKey getDataKey() throws DBException {
        if (dataKey == null) {
            dataKey = credentials.getDataKey();
        }
        return dataKey;
    }
}
