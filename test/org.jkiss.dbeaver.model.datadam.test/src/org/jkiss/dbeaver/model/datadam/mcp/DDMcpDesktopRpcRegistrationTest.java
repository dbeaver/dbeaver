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
package org.jkiss.dbeaver.model.datadam.mcp;

import com.dbeaver.datadam.dam.remote.api.DDRemoteDamSettingKeys;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSettings;
import com.dbeaver.datadam.mcp.api.service.desktop.DDMcpFunctionService;
import com.dbeaver.messaging.requestreply.MessagingSubscription;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class DDMcpDesktopRpcRegistrationTest {
    @Test
    void usesSettingsWithoutRemoteReadsAndClosesSubscriptionsOnDisableOrReconnect() {
        try (var functions = Mockito.mockConstruction(DDMcpDesktopFunctionService.class)) {
            var registration = new DDMcpDesktopRpcRegistration();
            var first = Mockito.mock(MessagingRpcEndpoint.class);
            var second = Mockito.mock(MessagingRpcEndpoint.class);
            var active = new AtomicInteger();
            for (var endpoint : new MessagingRpcEndpoint[] {first, second}) {
                Mockito.when(endpoint.register(Mockito.eq(DDMcpFunctionService.SERVICE_ID), Mockito.eq(DDMcpFunctionService.class),
                    Mockito.any(DDMcpFunctionService.class), Mockito.any())).thenAnswer(call -> {
                        active.incrementAndGet();
                        return (MessagingSubscription) active::decrementAndGet;
                    });
            }
            var enabled = new DDRemoteDamSettings(Map.of(), Map.of(DDRemoteDamSettingKeys.MCP_ENABLED, "true"));
            registration.register(first, DDRemoteDamSettings.EMPTY);
            Assertions.assertEquals(0, active.get());
            registration.register(first, enabled);
            registration.register(first, enabled);
            Assertions.assertEquals(1, active.get());
            registration.register(first, new DDRemoteDamSettings(Map.of(DDRemoteDamSettingKeys.MCP_ENABLED, "false"), enabled.dam()));
            Assertions.assertEquals(0, active.get());
            registration.register(first, enabled);
            registration.register(second, enabled);
            Assertions.assertEquals(1, active.get());
            registration.register(second, DDRemoteDamSettings.EMPTY);
            Assertions.assertEquals(0, active.get());
            Mockito.verify(first, Mockito.never()).proxy(Mockito.anyString(), Mockito.any());
        }
    }
}
