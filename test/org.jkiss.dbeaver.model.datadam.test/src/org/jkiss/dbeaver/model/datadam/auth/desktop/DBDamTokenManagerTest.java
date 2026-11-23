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
package org.jkiss.dbeaver.model.datadam.auth.desktop;

import com.dbeaver.datadam.sso.api.model.DDSsoTokenResponse;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DBDamSsoClient;
import org.jkiss.dbeaver.model.datadam.auth.DBDamSsoLogin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpConnectTimeoutException;
import java.util.Set;

class DBDamTokenManagerTest {
    @Test
    void refreshFailureThrowsWithoutOverwritingSavedCredentials() throws Exception {
        verifyRefreshFailure(new DBException("Offline", new IOException("Connection failed")), false);
    }

    @Test
    void explicitInvalidGrantDeletesSavedCredentials() throws Exception {
        verifyRefreshFailure(new DBDamSsoClient.InvalidGrantException(), true);
    }

    @Test
    void connectTimeoutCanRecoverWithoutRestartOrSignIn() throws Exception {
        var resource = URI.create("https://storage.example.test");
        var saved = new DBDamUserToken("access", "https://sso.example.test",
            Set.of(resource.toString()), Set.of("mcp:connect"), "refresh");
        var store = Mockito.mock(DBDamTokenStore.class);
        Mockito.when(store.hasSavedToken()).thenReturn(true);
        Mockito.when(store.load()).thenReturn(saved);
        var validator = Mockito.mock(DBDamTokenValidator.class);
        Mockito.when(validator.matchesResource(Mockito.any(), Mockito.eq(resource))).thenReturn(true);
        Mockito.when(validator.matchesIssuer(Mockito.any())).thenReturn(true);
        Mockito.when(validator.getIssuer("rotated-access")).thenReturn(saved.issuer());
        Mockito.when(validator.isExpired(saved)).thenReturn(true);
        var failure = new DBException("Offline", new HttpConnectTimeoutException("HTTP connect timed out"));
        var response = Mockito.mock(DDSsoTokenResponse.class);
        Mockito.when(response.accessToken()).thenReturn("rotated-access");
        Mockito.when(response.refreshToken()).thenReturn("rotated-refresh");
        try (var clients = Mockito.mockConstruction(DBDamSsoClient.class, (client, context) -> {
            if (context.getCount() == 1) {
                Mockito.when(client.refresh("refresh", "mcp:connect")).thenThrow(failure);
            } else {
                Mockito.when(client.refresh("refresh", "mcp:connect")).thenReturn(response);
            }
        })) {
            var manager = new DBDamTokenManager(store, validator, Mockito.mock(DBDamSsoLogin.class));

            Assertions.assertThrows(DBException.class, () -> manager.load(resource));
            Assertions.assertEquals(DBDamTokenManager.Status.SIGNED_IN, manager.getStatus());
            Assertions.assertEquals("rotated-access", manager.getAccessTokenForResource(resource));
            Assertions.assertEquals(2, clients.constructed().size());
            Mockito.verify(store).save(new DBDamUserToken("rotated-access", saved.issuer(),
                saved.endpoints(), saved.scopes(), "rotated-refresh"));
            Mockito.verify(store, Mockito.never()).clear();
        }
    }

    private void verifyRefreshFailure(@NotNull DBException failure, boolean clearExpected) throws Exception {
        var resource = URI.create("https://storage.example.test");
        var saved = new DBDamUserToken("access", "https://sso.example.test",
            Set.of(resource.toString()), Set.of("mcp:connect"), "refresh");
        var store = Mockito.mock(DBDamTokenStore.class);
        Mockito.when(store.hasSavedToken()).thenReturn(true);
        Mockito.when(store.load()).thenReturn(saved);
        var validator = Mockito.mock(DBDamTokenValidator.class);
        Mockito.when(validator.matchesResource(saved, resource)).thenReturn(true);
        Mockito.when(validator.matchesIssuer(saved)).thenReturn(true);
        Mockito.when(validator.isExpired(Mockito.any())).thenReturn(true);
        try (var clients = Mockito.mockConstruction(DBDamSsoClient.class, (client, context) ->
            Mockito.when(client.refresh("refresh", "mcp:connect")).thenThrow(failure))) {
            var manager = new DBDamTokenManager(store, validator, Mockito.mock(DBDamSsoLogin.class));

            Assertions.assertSame(failure, Assertions.assertThrows(DBException.class, () -> manager.load(resource)).getCause());

            Assertions.assertEquals(clearExpected ? DBDamTokenManager.Status.INVALID_TOKEN
                : DBDamTokenManager.Status.SIGNED_IN, manager.getStatus());
            Mockito.verify(store, Mockito.never()).save(Mockito.any());
            Mockito.verify(store, Mockito.times(clearExpected ? 1 : 0)).clear();
        }
    }
}
