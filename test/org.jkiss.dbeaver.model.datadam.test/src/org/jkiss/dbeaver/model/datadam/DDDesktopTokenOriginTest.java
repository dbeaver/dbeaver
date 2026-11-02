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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Set;

class DDDesktopTokenOriginTest {
    private static final String ACCOUNT = "https://account.staging.datadam.invalid";
    private static final String STORAGE = "https://dbeaver-storage.staging.datadam.invalid";
    private static final String REMOTE_DAM = "wss://remote-dam.staging.datadam.invalid/ws";
    private static final String PROD_ACCOUNT = "https://account.prod.datadam.invalid";
    private static final String PROD_STORAGE = "https://storage.prod.datadam.invalid";
    private static final String PROD_REMOTE_DAM = "wss://remote-dam.prod.datadam.invalid/ws";

    private MockedStatic<DDEndpoints> endpoints;
    private Constructor<?> tokenConstructor;
    private Object validator;
    private Method matchesResource;

    @BeforeEach
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    void setUp() throws ReflectiveOperationException {
        String desktopPackage = DDDesktopAccessService.class.getPackageName();
        Class<?> tokenType = Class.forName(desktopPackage + ".DDDesktopUserToken");
        tokenConstructor = tokenType.getDeclaredConstructor(String.class, String.class, Set.class, Set.class);
        tokenConstructor.setAccessible(true);
        Class<?> validatorType = Class.forName(desktopPackage + ".DDDesktopTokenValidator");
        Constructor<?> validatorConstructor = validatorType.getDeclaredConstructor();
        validatorConstructor.setAccessible(true);
        validator = validatorConstructor.newInstance();
        matchesResource = validatorType.getDeclaredMethod("matchesResource", tokenType, URI.class);
        matchesResource.setAccessible(true);
        endpoints = Mockito.mockStatic(DDEndpoints.class, Mockito.CALLS_REAL_METHODS);
        configure(ACCOUNT, STORAGE, REMOTE_DAM);
    }

    @AfterEach
    void tearDown() {
        if (endpoints != null) {
            endpoints.close();
        }
    }

    @Test
    void acceptsTheRemoteDamEndpointWithSavedServiceEndpoints() throws ReflectiveOperationException {
        Assertions.assertTrue(matches(ACCOUNT, STORAGE, REMOTE_DAM));
        Assertions.assertTrue(matches(ACCOUNT + ":443", STORAGE + ":443", REMOTE_DAM));
        Assertions.assertTrue(matches(ACCOUNT, STORAGE, "wss://remote-dam.staging.datadam.invalid:443/ws"));
        Assertions.assertTrue(matches(ACCOUNT, STORAGE, "WSS://REMOTE-DAM.STAGING.DATADAM.INVALID/ws"));
    }

    @Test
    void requiresBothSavedOriginsToMatchCurrentConfiguration() throws ReflectiveOperationException {
        Assertions.assertFalse(matches(PROD_ACCOUNT, STORAGE, REMOTE_DAM));
        Assertions.assertFalse(matches(ACCOUNT, PROD_STORAGE, REMOTE_DAM));
        Assertions.assertFalse(matches(PROD_ACCOUNT, PROD_STORAGE, REMOTE_DAM));
        Assertions.assertFalse(matches(ACCOUNT + ":8443", STORAGE, REMOTE_DAM));
        Assertions.assertFalse(matches(ACCOUNT, STORAGE + ":8443", REMOTE_DAM));

        configure(PROD_ACCOUNT, STORAGE, REMOTE_DAM);
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, REMOTE_DAM));
        configure(ACCOUNT, PROD_STORAGE, REMOTE_DAM);
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, REMOTE_DAM));
    }

    @Test
    void rejectsMissingOrPathRestrictedServiceEndpointsForWebSockets() throws ReflectiveOperationException {
        for (Set<String> allowed : List.of(Set.of(ACCOUNT), Set.of(STORAGE),
            Set.of(ACCOUNT + "/mcp", STORAGE), Set.of(ACCOUNT, STORAGE + "/mcp"))) {
            Object token = tokenConstructor.newInstance("existing-access-token", "https://sso.example", allowed, Set.of());
            Assertions.assertFalse((boolean) matchesResource.invoke(validator, token, URI.create(REMOTE_DAM)), allowed.toString());
        }
    }

    @Test
    void rejectsCrossEnvironmentWebSocketResources() throws ReflectiveOperationException {
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, PROD_REMOTE_DAM));
        configure(PROD_ACCOUNT, PROD_STORAGE, PROD_REMOTE_DAM);
        Assertions.assertTrue(matches(PROD_ACCOUNT, PROD_STORAGE, PROD_REMOTE_DAM));
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, PROD_REMOTE_DAM));
        Assertions.assertFalse(matches(PROD_ACCOUNT, PROD_STORAGE, REMOTE_DAM));
    }

    @Test
    void rejectsUntrustedWebSocketHostsPortsAndUnsafeUrls() throws ReflectiveOperationException {
        for (String resource : List.of(
            "wss://attacker.example/ws",
            "wss://remote-dam.staging.datadam.invalid.attacker.example/ws",
            "wss://other.staging.datadam.invalid/ws",
            "wss://remote-dam.staging.datadam.invalid:8443/ws",
            "wss://remote-dam.staging.datadam.invalid:80/ws",
            "wss://remote-dam.staging.datadam.invalid:0/ws",
            "wss://remote-dam.staging.datadam.invalid:65536/ws",
            "wss://remote-dam.staging.datadam.invalid:/ws",
            "wss://user:password@remote-dam.staging.datadam.invalid/ws",
            "wss://remote-dam.staging.datadam.invalid/other",
            "wss://remote-dam.staging.datadam.invalid/ws/",
            "wss://remote-dam.staging.datadam.invalid/w%73",
            "wss://remote-dam.staging.datadam.invalid/other/../ws",
            REMOTE_DAM + "?token=value",
            REMOTE_DAM + "?",
            REMOTE_DAM + "#fragment",
            REMOTE_DAM + "#",
            "ws://remote-dam.staging.datadam.invalid/ws",
            "ws://127.0.0.1/ws",
            "https://remote-dam.staging.datadam.invalid/ws",
            "ftp://remote-dam.staging.datadam.invalid/ws"
        )) {
            Assertions.assertFalse(matches(ACCOUNT, STORAGE, resource), resource);
        }
    }

    @Test
    void doesNotTrustWebSocketsOnSavedAccountOrStorageHosts() throws ReflectiveOperationException {
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, "wss://account.staging.datadam.invalid/ws"));
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, "wss://dbeaver-storage.staging.datadam.invalid/ws"));
    }

    @Test
    void preservesExistingHttpAccountAndStorageOriginMatching() throws ReflectiveOperationException {
        Assertions.assertTrue(matches(ACCOUNT, STORAGE, ACCOUNT + "/graphql"));
        Assertions.assertTrue(matches(ACCOUNT, STORAGE, STORAGE + "/graphql?query=value"));
        Assertions.assertTrue(matches(ACCOUNT + ":443", STORAGE + ":443", ACCOUNT + "/graphql"));
        Assertions.assertTrue(matches(ACCOUNT, STORAGE, STORAGE + ":443/graphql"));
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, ACCOUNT + ":8443/graphql"));
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, "https://user@account.staging.datadam.invalid/graphql"));
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, "ftp://account.staging.datadam.invalid/graphql"));
        Assertions.assertFalse(matches("ftp://account.staging.datadam.invalid", STORAGE, "ftp://account.staging.datadam.invalid/graphql"));
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, "http://account.staging.datadam.invalid/graphql"));
    }

    @Test
    void preservesSavedHttpOriginsWhenCurrentConfigurationDiffers() throws ReflectiveOperationException {
        configure(PROD_ACCOUNT, PROD_STORAGE, PROD_REMOTE_DAM);
        Assertions.assertTrue(matches(ACCOUNT, STORAGE, ACCOUNT + "/graphql"));
        Assertions.assertTrue(matches(ACCOUNT, STORAGE, STORAGE + "/graphql"));
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, PROD_REMOTE_DAM));
    }

    @Test
    void normalizesLoopbackHttpAndWebSocketDefaultPorts() throws ReflectiveOperationException {
        configure("http://localhost", "http://127.0.0.1:80", "ws://localhost:80/ws");
        Assertions.assertTrue(matches("http://localhost:80", "http://127.0.0.1", "ws://localhost/ws"));
        Assertions.assertTrue(matches("http://localhost", "http://127.0.0.1:80", "ws://localhost:80/ws"));
        Assertions.assertTrue(matches("http://localhost:80", "http://127.0.0.1", "http://localhost/graphql"));
        Assertions.assertTrue(matches("http://localhost", "http://127.0.0.1:80", "http://127.0.0.1/graphql"));
        Assertions.assertFalse(matches("http://localhost", "http://127.0.0.1", "wss://localhost/ws"));
        Assertions.assertFalse(matches("http://localhost", "http://127.0.0.1", "ws://localhost:8080/ws"));
        Assertions.assertFalse(matches("http://localhost", "http://127.0.0.1", "ws://127.0.0.1/ws"));
    }

    @Test
    void requiresTheConfiguredNonDefaultSecurePort() throws ReflectiveOperationException {
        configure(ACCOUNT, STORAGE, "wss://remote-dam.staging.datadam.invalid:8443/ws");
        Assertions.assertTrue(matches(ACCOUNT, STORAGE, "wss://remote-dam.staging.datadam.invalid:8443/ws"));
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, REMOTE_DAM));
        Assertions.assertFalse(matches(ACCOUNT, STORAGE, "wss://remote-dam.staging.datadam.invalid:443/ws"));
    }

    @Test
    void rejectsUnsafeConfiguredRemoteDamEndpoints() throws ReflectiveOperationException {
        for (String remoteDam : List.of(
            "ws://remote-dam.staging.datadam.invalid/ws",
            "https://remote-dam.staging.datadam.invalid/ws",
            "wss://user@remote-dam.staging.datadam.invalid/ws",
            "wss://remote-dam.staging.datadam.invalid/other",
            REMOTE_DAM + "?token=value",
            REMOTE_DAM + "#fragment"
        )) {
            configure(ACCOUNT, STORAGE, remoteDam);
            Assertions.assertFalse(matches(ACCOUNT, STORAGE, REMOTE_DAM), remoteDam);
        }
    }

    @Test
    void rejectsMalformedSavedOriginsForTheRemoteDamEndpoint() throws ReflectiveOperationException {
        for (String account : List.of(
            "not-a-url", "https:/account.staging.datadam.invalid", "https://user@account.staging.datadam.invalid"
        )) {
            Assertions.assertFalse(matches(account, STORAGE, REMOTE_DAM), account);
        }
        Assertions.assertFalse(matches(ACCOUNT, "https://user@dbeaver-storage.staging.datadam.invalid", REMOTE_DAM));
    }

    private void configure(@NotNull String account, @NotNull String storage, @NotNull String remoteDam) {
        endpoints.when(DDEndpoints::getAccountBaseUrl).thenReturn(account);
        endpoints.when(DDEndpoints::getStorageBaseUrl).thenReturn(storage);
        endpoints.when(DDEndpoints::getRemoteDamWebSocketUrl).thenReturn(remoteDam);
    }

    private boolean matches(@NotNull String account, @NotNull String storage, @NotNull String resource)
            throws ReflectiveOperationException {
        Object token = tokenConstructor.newInstance("existing-access-token", "https://sso.example", Set.of(account, storage), Set.of());
        return (boolean) matchesResource.invoke(validator, token, URI.create(resource));
    }
}
