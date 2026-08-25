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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.SecretKey;

/**
 * Encrypted view over a transport: resources are packed and encrypted here,
 * the transport below sees opaque bytes only.
 */
public class DDSyncStore {

    private static final int SCHEMA_VERSION = 1;

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
    public List<DDConfigurationSummary> listConfigurations() throws DBException {
        List<DDConfigurationSummary> configurations = new ArrayList<>();
        for (DDConfigurationSummaryData data : transport.listConfigurations()) {
            configurations.add(new DDConfigurationSummary(data.configurationId(), data.name(), data.version()));
        }
        return configurations;
    }

    @NotNull
    public DDConfiguration getConfiguration(@NotNull String configurationId) throws DBException {
        return decode(transport.getConfiguration(configurationId));
    }

    @NotNull
    public DDConfiguration createConfiguration(
        @NotNull String name,
        @NotNull List<DDConfigurationPart> parts
    ) throws DBException {
        List<DDCreateConfigurationPartRequest> requests = new ArrayList<>(parts.size());
        for (DDConfigurationPart part : parts) {
            requests.add(new DDCreateConfigurationPartRequest(
                part.key(), part.kind().name(), part.projectId(), encrypt(part)));
        }
        return decode(transport.createConfiguration(new DDCreateConfigurationRequest(name, requests)));
    }

    @NotNull
    public DDConfiguration updateConfiguration(
        @NotNull String configurationId,
        long expectedVersion,
        @NotNull List<DDConfigurationPart> parts
    ) throws DBException {
        List<DDUpdateConfigurationPartRequest> requests = new ArrayList<>(parts.size());
        for (DDConfigurationPart part : parts) {
            requests.add(new DDUpdateConfigurationPartRequest(part.key(), part.version(), encrypt(part)));
        }
        return decode(transport.updateConfiguration(
            configurationId, new DDUpdateConfigurationRequest(expectedVersion, requests)));
    }

    @NotNull
    public String fingerprint(@NotNull DDConfigurationPart part) throws DBException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(serialize(part));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new DBException("SHA-256 is not available", e);
        }
    }

    @NotNull
    private DDConfiguration decode(@NotNull DDConfigurationData data) throws DBException {
        List<DDConfigurationPart> parts = new ArrayList<>(data.parts().size());
        for (DDConfigurationPartData part : data.parts()) {
            parts.add(decode(part));
        }
        return new DDConfiguration(data.configurationId(), data.name(), data.version(), parts);
    }

    @NotNull
    private DDConfigurationPart decode(@NotNull DDConfigurationPartData part) throws DBException {
        byte[] encrypted = Base64.getDecoder().decode(part.encryptedValue());
        DDPartEnvelope envelope = JSONUtils.GSON.fromJson(
            new String(DDCrypto.decrypt(getDataKey(), encrypted), StandardCharsets.UTF_8),
            DDPartEnvelope.class);
        if (envelope.schemaVersion() != SCHEMA_VERSION) {
            throw new DBException("Unsupported synchronization part format: " + envelope.schemaVersion());
        }
        return new DDConfigurationPart(
            part.key(),
            DDConfigurationPartKind.valueOf(part.kind()),
            part.projectId(),
            part.version(),
            envelope.name(),
            decodeUnits(envelope.units()));
    }

    @NotNull
    private String encrypt(@NotNull DDConfigurationPart part) throws DBException {
        return Base64.getEncoder().encodeToString(DDCrypto.encrypt(getDataKey(), serialize(part)));
    }

    @NotNull
    private static byte[] serialize(@NotNull DDConfigurationPart part) {
        Map<String, Map<String, String>> units = new TreeMap<>();
        for (Map.Entry<String, Map<String, byte[]>> unit : part.units().entrySet()) {
            Map<String, String> resources = new TreeMap<>();
            for (Map.Entry<String, byte[]> resource : unit.getValue().entrySet()) {
                resources.put(resource.getKey(), Base64.getEncoder().encodeToString(resource.getValue()));
            }
            units.put(unit.getKey(), resources);
        }
        return JSONUtils.GSON.toJson(new DDPartEnvelope(SCHEMA_VERSION, part.name(), units))
            .getBytes(StandardCharsets.UTF_8);
    }

    @NotNull
    private static Map<String, Map<String, byte[]>> decodeUnits(@NotNull Map<String, Map<String, String>> encoded) {
        Map<String, Map<String, byte[]>> units = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> unit : encoded.entrySet()) {
            Map<String, byte[]> resources = new LinkedHashMap<>();
            for (Map.Entry<String, String> resource : unit.getValue().entrySet()) {
                resources.put(resource.getKey(), Base64.getDecoder().decode(resource.getValue()));
            }
            units.put(unit.getKey(), resources);
        }
        return units;
    }

    @NotNull
    private SecretKey getDataKey() throws DBException {
        if (dataKey == null) {
            dataKey = credentials.getDataKey();
        }
        return dataKey;
    }
}
