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
package org.jkiss.dbeaver.model.datadam;

import com.dbeaver.datadam.dam.remote.api.model.DDConnectedRemoteDam;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSettings;
import com.google.gson.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCryptoState;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.utils.GsonUtils;
import org.jkiss.utils.HttpConstants;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Account requests authenticated by the current desktop SSO session. */
public class DDAccountClient extends DDClient {
    private static final String GRAPHQL_PATH = "graphql";
    private static final String ACCOUNT_INFO_QUERY = """
        query AccountInfo {
            accountInfo {
                accountId accountName
            }
        }
        """;
    private static final String ACCOUNT_ID_QUERY = """
        query GatewayAccountId {
            gatewayCryptoState {
                accountId
            }
        }
        """;
    private static final String CRYPTO_STATE_QUERY = """
        query GatewayCryptoState {
            gatewayCryptoState {
                accountId cryptoConfigured encryptedBundle generation salt iterations
            }
        }
        """;
    private static final String DAM_SETTINGS_QUERY = """
        query DamSettings($clientDamId: String!) {
            accountInfo { accountId }
            damSettings(clientDamId: $clientDamId) { account dam }
            canManageSettings
        }
        """;
    private static final String SET_DAM_SETTING = """
        mutation SetDamSetting($clientDamId: String!, $key: String!, $value: String!, $expectedAccountId: ID!) {
            setDamSetting(clientDamId: $clientDamId, key: $key, value: $value, expectedAccountId: $expectedAccountId) { account dam }
        }
        """;
    private static final String DAM_FIELDS = """
        damId appDeploymentId appInstanceId workspaceId remoteAddress online sessionCount
        applications { product version }
        firstConnectedAt lastConnectedAt lastDisconnectedAt clientDamId remoteAddresses damName
        """;
    private static final String DAM_NAME_QUERY = """
        query DesktopDamName($clientDamId: String!) {
            connectedRemoteDamByClientId(clientDamId: $clientDamId) {
                %s
            }
        }
        """.formatted(DAM_FIELDS);
    private static final String RENAME_DAM = """
        mutation RenameDesktopDam($clientDamId: String!, $damName: String!) {
            renameConnectedRemoteDamByClientId(clientDamId: $clientDamId, damName: $damName) {
                %s
            }
        }
        """.formatted(DAM_FIELDS);

    public DDAccountClient(@NotNull URI accountUrl) {
        super(accountUrl.resolve("/").toString(), DEFAULT_CONNECT_TIMEOUT, 10_000, List.of(), HttpClient.Redirect.NEVER);
        gson = GsonUtils.gsonBuilder().registerTypeAdapter(OffsetDateTime.class,
            (JsonDeserializer<OffsetDateTime>) (json, type, context) -> OffsetDateTime.parse(json.getAsString())).create();
    }

    @NotNull
    public DDCryptoState getCryptoState() throws DBException {
        CryptoResponse response = executePostRequest(GRAPHQL_PATH, Map.of(), Map.of("query", CRYPTO_STATE_QUERY), CryptoResponse.class);
        if (response == null || response.errors() != null && !response.errors().isEmpty()
            || response.data() == null || response.data().gatewayCryptoState() == null) {
            throw responseError("Could not read encryption keys from DataDam Account", response == null ? null : response.errors());
        }
        DDCryptoState state = response.data().gatewayCryptoState();
        if (state.accountId() == null) {
            throw new DBException("DataDam Account response has no account ID");
        }
        return state;
    }

    @NotNull
    public UUID getAccountId() throws DBException {
        AccountResponse response = executePostRequest(
            GRAPHQL_PATH, Map.of(), Map.of("query", ACCOUNT_ID_QUERY), AccountResponse.class);
        if (response == null || response.errors() != null && !response.errors().isEmpty()
            || response.data() == null || response.data().gatewayCryptoState() == null
            || response.data().gatewayCryptoState().accountId() == null) {
            throw responseError("Could not read account ID from DataDam Account", response == null ? null : response.errors());
        }
        return response.data().gatewayCryptoState().accountId();
    }

    @NotNull
    public String getAccountName() throws DBException {
        AccountResponse response = executePostRequest(
            GRAPHQL_PATH, Map.of(), Map.of("query", ACCOUNT_INFO_QUERY), AccountResponse.class);
        if (response == null || response.errors() != null && !response.errors().isEmpty()
            || response.data() == null || response.data().accountInfo() == null
            || response.data().accountInfo().accountName() == null) {
            throw responseError("Could not read account name from DataDam Account", response == null ? null : response.errors());
        }
        return response.data().accountInfo().accountName();
    }

    @NotNull
    public DamSettingsState getDamSettings(@NotNull String clientDamId) throws DBException {
        DamSettingsResponse response = executePostRequest(GRAPHQL_PATH, Map.of(), Map.of(
            "query", DAM_SETTINGS_QUERY, "variables", Map.of("clientDamId", clientDamId)), DamSettingsResponse.class);
        if (response == null || response.errors() != null && !response.errors().isEmpty()
            || response.data() == null || response.data().accountInfo() == null
            || response.data().accountInfo().accountId() == null || response.data().damSettings() == null
            || response.data().canManageSettings() == null) {
            throw responseError("Could not read DAM settings from DataDam Account", response == null ? null : response.errors());
        }
        var data = response.data();
        return new DamSettingsState(data.accountInfo().accountId(), data.damSettings(), data.canManageSettings());
    }

    @NotNull
    public DDRemoteDamSettings setDamSetting(
        @NotNull String clientDamId, @NotNull String key, @NotNull String value, @NotNull UUID expectedAccountId
    ) throws DBException {
        DamSettingsResponse response = executePostRequest(GRAPHQL_PATH, Map.of(), Map.of(
            "query", SET_DAM_SETTING, "variables", Map.of(
                "clientDamId", clientDamId, "key", key, "value", value, "expectedAccountId", expectedAccountId.toString())),
            DamSettingsResponse.class);
        if (response == null || response.errors() != null && !response.errors().isEmpty()
            || response.data() == null || response.data().setDamSetting() == null) {
            throw responseError("Could not update DAM settings in DataDam Account", response == null ? null : response.errors());
        }
        return response.data().setDamSetting();
    }

    @Nullable
    public DDConnectedRemoteDam getDam(@NotNull String clientDamId) throws DBException {
        return requestDam(DAM_NAME_QUERY, Map.of("clientDamId", clientDamId), "connectedRemoteDamByClientId");
    }

    @NotNull
    public DDConnectedRemoteDam renameDam(@NotNull String clientDamId, @NotNull String damName) throws DBException {
        DDConnectedRemoteDam result = requestDam(RENAME_DAM, Map.of("clientDamId", clientDamId, "damName", damName),
            "renameConnectedRemoteDamByClientId");
        if (result == null || result.damId() == null || result.damName() == null) {
            throw new DBException("Could not update DAM name in DataDam Account");
        }
        return result;
    }

    @Nullable
    private DDConnectedRemoteDam requestDam(
        @NotNull String query, @NotNull Map<String, String> variables, @NotNull String field
    ) throws DBException {
        JsonObject response = executePostRequest(GRAPHQL_PATH, Map.of(), Map.of(
            "query", query, "variables", variables), JsonObject.class);
        JsonArray errors = response != null && response.get("errors") instanceof JsonArray array ? array : null;
        if (response == null || errors != null && !errors.isEmpty()
            || !response.has("data") || !response.get("data").isJsonObject()) {
            throw responseError("Could not read DAM from DataDam Account", errors);
        }
        JsonElement dam = response.getAsJsonObject("data").get(field);
        if (dam == null) {
            throw new DBException("Invalid DAM response from DataDam Account");
        }
        try {
            return dam.isJsonNull() ? null : gson.fromJson(dam, DDConnectedRemoteDam.class);
        } catch (JsonParseException e) {
            throw new DBException("Invalid DAM response from DataDam Account", e);
        }
    }

    @NotNull
    private DBException responseError(@NotNull String message, @Nullable JsonArray errors) {
        if (errors != null && !errors.isEmpty() && errors.get(0) instanceof JsonObject first
            && first.get("message") instanceof JsonPrimitive detail) {
            return new DBException(message + ": " + detail.getAsString());
        }
        return new DBException(message);
    }

    public record DamSettingsState(@NotNull UUID accountId, @NotNull DDRemoteDamSettings settings, boolean canManage) {
    }

    private record DamSettingsResponse(@Nullable DamSettingsData data, @Nullable JsonArray errors) {
    }

    private record DamSettingsData(
        @Nullable AccountState accountInfo, @Nullable DDRemoteDamSettings damSettings,
        @Nullable DDRemoteDamSettings setDamSetting, @Nullable Boolean canManageSettings
    ) {
    }

    @NotNull
    @Override
    protected <T> T execute(@NotNull HttpRequest.Builder builder, @NotNull Type type) throws DBException {
        URI uri = builder.build().uri();
        DDEndpoints.requireSecureTransport(uri);
        builder.setHeader(HttpConstants.HEADER_AUTHORIZATION,
            HttpConstants.BEARER_PREFIX + DDDesktopAccessService.getInstance().getAccessTokenForResource(uri));
        return super.execute(builder, type);
    }

    @NotNull
    @Override
    protected DBException mapErrorResponse(int code, @NotNull String message, @NotNull URI uri) {
        return new DBException("DataDam Account request failed (HTTP " + code + ")");
    }

    @Override
    protected void handleRequestException(@NotNull String message, @NotNull Throwable e) throws DBException {
        if (e instanceof JsonParseException) {
            throw new DBException("Invalid DataDam Account response");
        }
        super.handleRequestException(message, e);
    }

    private record CryptoResponse(@Nullable CryptoData data, @Nullable JsonArray errors) {
    }

    private record CryptoData(@Nullable DDCryptoState gatewayCryptoState) {
    }

    private record AccountResponse(@Nullable AccountData data, @Nullable JsonArray errors) {
    }

    private record AccountData(@Nullable AccountState gatewayCryptoState, @Nullable AccountState accountInfo) {
    }

    private record AccountState(@Nullable UUID accountId, @Nullable String accountName) {
    }
}
