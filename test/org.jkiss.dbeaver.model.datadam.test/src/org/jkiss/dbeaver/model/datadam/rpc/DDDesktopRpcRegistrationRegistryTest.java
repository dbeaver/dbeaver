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
package org.jkiss.dbeaver.model.datadam.rpc;

import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSettings;
import com.dbeaver.datadam.dam.remote.api.service.desktop.DDRemoteDamSettingsService;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.jkiss.dbeaver.DBException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class DDDesktopRpcRegistrationRegistryTest {
    @Test
    void serializesInitialSettingsBeforeAnEarlyCallback() throws Exception {
        var registry = Mockito.mock(DDDesktopRpcRegistrationRegistry.class);
        var endpoint = Mockito.mock(MessagingRpcEndpoint.class);
        var initial = new DDRemoteDamSettings(Map.of(), Map.of("feature", "false"));
        var updated = new DDRemoteDamSettings(Map.of(), Map.of("feature", "true"));
        var callback = new AtomicReference<DDRemoteDamSettingsService>();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        List<DDRemoteDamSettings> applied = new CopyOnWriteArrayList<>();
        Mockito.doCallRealMethod().when(registry).registerEndpoint(endpoint, initial);
        Mockito.when(endpoint.register(Mockito.anyString(), Mockito.eq(DDRemoteDamSettingsService.class), Mockito.any()))
            .thenAnswer(call -> {
                callback.set(call.getArgument(2));
                return null;
            });
        Mockito.doAnswer(call -> {
            if (call.getArgument(1).equals(initial)) {
                entered.countDown();
                Assertions.assertTrue(release.await(3, TimeUnit.SECONDS));
            }
            applied.add(call.getArgument(1));
            return null;
        }).when(registry).registerAll(Mockito.eq(endpoint), Mockito.any());
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var start = workers.submit(() -> {
                registry.registerEndpoint(endpoint, initial);
                return null;
            });
            Assertions.assertTrue(entered.await(3, TimeUnit.SECONDS));
            var update = workers.submit(() -> {
                callback.get().updateSettings(updated);
                return null;
            });
            release.countDown();
            start.get(3, TimeUnit.SECONDS);
            update.get(3, TimeUnit.SECONDS);
            Assertions.assertEquals(List.of(initial, updated), applied);
        } finally {
            release.countDown();
        }
    }

    @Test
    void callbackCanRecoverWhenInitialRegistrationFails() throws Exception {
        var registry = Mockito.mock(DDDesktopRpcRegistrationRegistry.class);
        var endpoint = Mockito.mock(MessagingRpcEndpoint.class);
        var callback = new AtomicReference<DDRemoteDamSettingsService>();
        Mockito.doCallRealMethod().when(registry).registerEndpoint(endpoint, DDRemoteDamSettings.EMPTY);
        Mockito.when(endpoint.register(Mockito.anyString(), Mockito.eq(DDRemoteDamSettingsService.class), Mockito.any()))
            .thenAnswer(call -> {
                callback.set(call.getArgument(2));
                return null;
            });
        Mockito.doThrow(new DBException("initial registration failed")).doNothing()
            .when(registry).registerAll(endpoint, DDRemoteDamSettings.EMPTY);
        Assertions.assertThrows(DBException.class, () -> registry.registerEndpoint(endpoint, DDRemoteDamSettings.EMPTY));
        callback.get().updateSettings(DDRemoteDamSettings.EMPTY);
        Mockito.verify(registry, Mockito.times(2)).registerAll(endpoint, DDRemoteDamSettings.EMPTY);
    }
}
