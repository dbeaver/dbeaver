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
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.ToNumberPolicy;
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
            connectedRemoteDam: connectedRemoteDamByClientId(clientDamId: $clientDamId) {
                %s
            }
        }
        """.formatted(DAM_FIELDS);
    private static final String RENAME_DAM = """
        mutation RenameDesktopDam($clientDamId: String!, $damName: String!) {
            connectedRemoteDam: renameConnectedRemoteDamByClientId(clientDamId: $clientDamId, damName: $damName) {
                %s
            }
        }
        """.formatted(DAM_FIELDS);

    public DDAccountClient(@NotNull URI accountUrl) {
        super(accountUrl.resolve("/").toString(), DEFAULT_CONNECT_TIMEOUT, 10_000, List.of(), HttpClient.Redirect.NEVER);
        gson = GsonUtils.gsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class,
                (JsonDeserializer<OffsetDateTime>) (json, type, context) -> OffsetDateTime.parse(json.getAsString()))
            .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)
            .create();
    }

    @NotNull
    public DDCryptoState getCryptoState() throws DBException {
        CryptoData data = executeGraphQl(CRYPTO_STATE_QUERY, Map.of(), CryptoData.class,
            "Could not read encryption keys from DataDam Account");
        if (data.gatewayCryptoState() == null) {
            throw new DBException("Could not read encryption keys from DataDam Account");
        }
        DDCryptoState state = data.gatewayCryptoState();
        if (state.accountId() == null) {
            throw new DBException("DataDam Account response has no account ID");
        }
        return state;
    }

    @NotNull
    public UUID getAccountId() throws DBException {
        AccountData data = executeGraphQl(ACCOUNT_ID_QUERY, Map.of(), AccountData.class,
            "Could not read account ID from DataDam Account");
        if (data.gatewayCryptoState() == null || data.gatewayCryptoState().accountId() == null) {
            throw new DBException("Could not read account ID from DataDam Account");
        }
        return data.gatewayCryptoState().accountId();
    }

    @NotNull
    public String getAccountName() throws DBException {
        AccountData data = executeGraphQl(ACCOUNT_INFO_QUERY, Map.of(), AccountData.class,
            "Could not read account name from DataDam Account");
        if (data.accountInfo() == null || data.accountInfo().accountName() == null) {
            throw new DBException("Could not read account name from DataDam Account");
        }
        return data.accountInfo().accountName();
    }

    @NotNull
    public DamSettingsState getDamSettings(@NotNull String clientDamId) throws DBException {
        DamSettingsData data = executeGraphQl(DAM_SETTINGS_QUERY, Map.of("clientDamId", clientDamId), DamSettingsData.class,
            "Could not read DAM settings from DataDam Account");
        if (data.accountInfo() == null || data.accountInfo().accountId() == null || data.damSettings() == null
            || data.canManageSettings() == null) {
            throw new DBException("Could not read DAM settings from DataDam Account");
        }
        return new DamSettingsState(data.accountInfo().accountId(), data.damSettings(), data.canManageSettings());
    }

    @NotNull
    public DDRemoteDamSettings setDamSetting(
        @NotNull String clientDamId, @NotNull String key, @NotNull String value, @NotNull UUID expectedAccountId
    ) throws DBException {
        DamSettingsData data = executeGraphQl(SET_DAM_SETTING, Map.of(
            "clientDamId", clientDamId, "key", key, "value", value, "expectedAccountId", expectedAccountId.toString()),
            DamSettingsData.class, "Could not update DAM settings in DataDam Account");
        if (data.setDamSetting() == null) {
            throw new DBException("Could not update DAM settings in DataDam Account");
        }
        return data.setDamSetting();
    }

    @Nullable
    public DDConnectedRemoteDam getDam(@NotNull String clientDamId) throws DBException {
        DamData data = executeGraphQl(DAM_NAME_QUERY, Map.of("clientDamId", clientDamId), DamData.class,
            "Could not read DAM from DataDam Account");
        return data.connectedRemoteDam();
    }

    @NotNull
    public DDConnectedRemoteDam renameDam(@NotNull String clientDamId, @NotNull String damName) throws DBException {
        DamData data = executeGraphQl(RENAME_DAM, Map.of("clientDamId", clientDamId, "damName", damName), DamData.class,
            "Could not update DAM name in DataDam Account");
        DDConnectedRemoteDam result = data.connectedRemoteDam();
        if (result == null || result.damId() == null || result.damName() == null) {
            throw new DBException("Could not update DAM name in DataDam Account");
        }
        return result;
    }

    @NotNull
    private <T> T executeGraphQl(
        @NotNull String query, @NotNull Map<String, ?> variables, @NotNull Class<T> dataType, @NotNull String errorMessage
    ) throws DBException {
        Map<String, ?> body = variables.isEmpty() ? Map.of("query", query) : Map.of("query", query, "variables", variables);
        Map<?, ?> response = executePostRequest(GRAPHQL_PATH, Map.of(), body, Map.class);
        List<?> errors = response != null && response.get("errors") instanceof List<?> list ? list : null;
        if (response == null || errors != null && !errors.isEmpty() || !(response.get("data") instanceof Map<?, ?> data)) {
            throw responseError(errorMessage, errors);
        }
        return convertGraphQlData(data, dataType);
    }

    @NotNull
    private <T> T convertGraphQlData(@NotNull Object data, @NotNull Class<T> dataType) throws DBException {
        if (dataType.isInstance(data)) {
            return dataType.cast(data);
        }
        try {
            return gson.fromJson(gson.toJsonTree(data), dataType);
        } catch (JsonParseException e) {
            throw new DBException("Invalid DataDam Account response", e);
        }
    }

    @NotNull
    private DBException responseError(@NotNull String message, @Nullable List<?> errors) {
        if (errors != null && !errors.isEmpty() && errors.get(0) instanceof Map<?, ?> first
            && first.get("message") instanceof String detail) {
            return new DBException(message + ": " + detail);
        }
        return new DBException(message);
    }

    public record DamSettingsState(@NotNull UUID accountId, @NotNull DDRemoteDamSettings settings, boolean canManage) {
    }

    private record DamSettingsData(
        @Nullable AccountState accountInfo, @Nullable DDRemoteDamSettings damSettings,
        @Nullable DDRemoteDamSettings setDamSetting, @Nullable Boolean canManageSettings
    ) {
    }

    private record DamData(@Nullable DDConnectedRemoteDam connectedRemoteDam) {
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

    private record CryptoData(@Nullable DDCryptoState gatewayCryptoState) {
    }

    private record AccountData(@Nullable AccountState gatewayCryptoState, @Nullable AccountState accountInfo) {
    }

    private record AccountState(@Nullable UUID accountId, @Nullable String accountName) {
    }
}
