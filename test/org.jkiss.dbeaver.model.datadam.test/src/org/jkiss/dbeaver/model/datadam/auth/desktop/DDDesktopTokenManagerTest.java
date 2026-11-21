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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoClient;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoLogin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

class DDDesktopTokenManagerTest {
    @Test
    void refreshFailureThrowsWithoutOverwritingSavedCredentials() throws Exception {
        verifyRefreshFailure(new DBException("Offline", new IOException("Connection failed")), false);
    }

    @Test
    void explicitInvalidGrantDeletesSavedCredentials() throws Exception {
        verifyRefreshFailure(new DDDesktopSsoClient.InvalidGrantException(), true);
    }

    private void verifyRefreshFailure(@NotNull DBException failure, boolean clearExpected) throws Exception {
        var resource = URI.create("https://storage.example.test");
        var saved = new DDDesktopUserToken("access", "https://sso.example.test",
            Set.of(resource.toString()), Set.of("mcp:connect"), "refresh");
        var store = Mockito.mock(DDDesktopTokenStore.class);
        Mockito.when(store.hasSavedToken()).thenReturn(true);
        Mockito.when(store.load()).thenReturn(saved);
        var validator = Mockito.mock(DDDesktopTokenValidator.class);
        Mockito.when(validator.matchesResource(saved, resource)).thenReturn(true);
        Mockito.when(validator.matchesIssuer(saved)).thenReturn(true);
        Mockito.when(validator.isExpired(Mockito.any())).thenReturn(true);
        try (var clients = Mockito.mockConstruction(DDDesktopSsoClient.class, (client, context) ->
            Mockito.when(client.refresh("refresh", "mcp:connect")).thenThrow(failure))) {
            var manager = new DDDesktopTokenManager(store, validator, Mockito.mock(DDDesktopSsoLogin.class));

            Assertions.assertSame(failure, Assertions.assertThrows(DBException.class, () -> manager.load(resource)).getCause());

            Assertions.assertEquals(DDDesktopTokenManager.Status.INVALID_TOKEN, manager.getStatus());
            Mockito.verify(store, Mockito.never()).save(Mockito.any());
            Mockito.verify(store, Mockito.times(clearExpected ? 1 : 0)).clear();
        }
    }
}
