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

import com.google.gson.JsonParser;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DDDesktopTokenValidatorTest {
    private final DDDesktopTokenValidator validator = new DDDesktopTokenValidator();
    private final DDDesktopUserToken api = new DDDesktopUserToken("api-token", "https://sso.example.test",
        Set.of("https://account.example.test", "https://storage.example.test"),
        Set.of("account:full_access", "dbeaver-storage:full_access", "mcp:connect"));
    private final DDDesktopUserToken mcp = new DDDesktopUserToken("mcp-token", "https://sso.example.test",
        Set.of("https://mcp.example.test/mcp"), Set.of("mcp:connect"));

    @Test
    void storesReplacesAndClearsOneTokenUnderOriginalSecretId() throws Exception {
        Map<String, String> secrets = new HashMap<>();
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        Mockito.when(controller.getPrivateSecretValue(Mockito.anyString()))
            .thenAnswer(invocation -> secrets.get(invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> {
            secrets.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(controller).setPrivateSecretValue(Mockito.anyString(), Mockito.any());
        // The store is package-private in another OSGi classloader; keep its implementation out of the public API.
        var type = Class.forName(DDDesktopUserToken.class.getPackageName() + ".DDDesktopTokenStore",
            true, DDDesktopUserToken.class.getClassLoader());
        var constructor = type.getDeclaredConstructor(DBSSecretController.class);
        var load = type.getDeclaredMethod("load");
        var save = type.getDeclaredMethod("save", DDDesktopUserToken.class);
        final var clear = type.getDeclaredMethod("clear");
        constructor.setAccessible(true);
        load.setAccessible(true);
        save.setAccessible(true);
        clear.setAccessible(true);
        var store = constructor.newInstance(controller);
        Assertions.assertNull(load.invoke(store));
        save.invoke(store, api);
        Assertions.assertEquals(Set.of("datadam.desktop.access-token"), secrets.keySet());
        Assertions.assertTrue(JsonParser.parseString(secrets.get("datadam.desktop.access-token")).isJsonObject());
        var restored = constructor.newInstance(controller);
        var stored = (DDDesktopUserToken) load.invoke(restored);
        Assertions.assertEquals(api, stored);
        Assertions.assertFalse(validator.matchesResource(api, URI.create("https://mcp.example.test/mcp")));
        Assertions.assertFalse(validator.matchesResource(mcp, URI.create("https://account.example.test/graphql")));
        Assertions.assertTrue(validator.matchesResource(stored, URI.create("https://account.example.test/graphql")));
        Assertions.assertTrue(validator.matchesResource(stored, URI.create("https://storage.example.test/projects")));
        save.invoke(restored, mcp);
        Assertions.assertEquals(mcp, load.invoke(store));
        Assertions.assertEquals(1, secrets.size());
        clear.invoke(restored);
        Assertions.assertNull(load.invoke(store));
        Assertions.assertNull(secrets.get("datadam.desktop.access-token"));
        Mockito.verify(controller, Mockito.times(3)).flushChanges();
    }

    @Test
    void emptyEndpointPathMatchesRootAndChildRequests() {
        URI endpoint = URI.create("https://account.example.test");
        Assertions.assertEquals("", endpoint.getPath());
        Assertions.assertTrue(validator.matchesResource(api, endpoint));
        Assertions.assertTrue(validator.matchesResource(api, endpoint.resolve("/")));
        Assertions.assertTrue(validator.matchesResource(api, endpoint.resolve("/graphql")));
    }

    @Test
    void endpointMatchingHonorsPathBoundariesAndDecodedTraversal() {
        Assertions.assertTrue(validator.matchesResource(mcp, URI.create("https://mcp.example.test/mcp")));
        Assertions.assertTrue(validator.matchesResource(mcp, URI.create("https://mcp.example.test:443/mcp/connect")));
        for (String endpoint : List.of("https://mcp.example.test/mcp-other", "https://mcp.example.test/mcp/../account",
            "https://mcp.example.test/mcp/%2e%2e/account", "https://mcp.example.test:8443/mcp",
            "http://mcp.example.test/mcp", "https://mcp.example.test.evil.test/mcp")) {
            Assertions.assertFalse(validator.matchesResource(mcp, URI.create(endpoint)), endpoint);
        }
    }
}
