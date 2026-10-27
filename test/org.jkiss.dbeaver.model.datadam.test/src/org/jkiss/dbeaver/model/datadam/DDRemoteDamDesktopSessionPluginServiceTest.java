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

import org.jkiss.dbeaver.DBException;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DDRemoteDamDesktopSessionPluginServiceTest extends DBeaverUnitTest {
    @Test
    void activatesAndClosesTheRetainedServiceOnlyOnce() throws Exception {
        var lifecycle = new DDRemoteDamDesktopSessionPluginService();
        var session = Mockito.mock(DDRemoteDamDesktopSessionService.class);
        try (var services = Mockito.mockStatic(DDRemoteDamDesktopSessionService.class)) {
            services.when(DDRemoteDamDesktopSessionService::getInstance).thenReturn(session);

            lifecycle.activateService();
            lifecycle.activateService();
            lifecycle.deactivateService();
            lifecycle.deactivateService();
            lifecycle.activateService();

            var order = Mockito.inOrder(session);
            order.verify(session).start();
            order.verify(session).close();
            Mockito.verifyNoMoreInteractions(session);
            services.verify(DDRemoteDamDesktopSessionService::getInstance);
            services.verifyNoMoreInteractions();
        }
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void shutdownWithoutActivationNeverCreatesASessionService() {
        var lifecycle = new DDRemoteDamDesktopSessionPluginService();
        try (var services = Mockito.mockStatic(DDRemoteDamDesktopSessionService.class)) {
            lifecycle.deactivateService();
            lifecycle.deactivateService();
            lifecycle.activateService();

            services.verifyNoInteractions();
        }
    }

    @Test
    void initializationFailureDoesNotCreateAServiceDuringShutdown() throws Exception {
        var lifecycle = new DDRemoteDamDesktopSessionPluginService();
        try (var services = Mockito.mockStatic(DDRemoteDamDesktopSessionService.class)) {
            services.when(DDRemoteDamDesktopSessionService::getInstance).thenThrow(new DBException("Initialization failed"));

            Assertions.assertDoesNotThrow(lifecycle::activateService);
            lifecycle.deactivateService();
            lifecycle.activateService();

            services.verify(DDRemoteDamDesktopSessionService::getInstance);
            services.verifyNoMoreInteractions();
        }
    }

    @Test
    void rollsBackPartialActivationWithoutLeavingAServiceForShutdown() throws Exception {
        var lifecycle = new DDRemoteDamDesktopSessionPluginService();
        var session = Mockito.mock(DDRemoteDamDesktopSessionService.class);
        var failure = new IllegalStateException("Activation failed");
        Mockito.doThrow(failure).when(session).start();
        try (var services = Mockito.mockStatic(DDRemoteDamDesktopSessionService.class)) {
            services.when(DDRemoteDamDesktopSessionService::getInstance).thenReturn(session);

            Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, lifecycle::activateService));
            lifecycle.deactivateService();
            lifecycle.activateService();

            Mockito.verify(session).start();
            Mockito.verify(session).close();
            Mockito.verifyNoMoreInteractions(session);
            services.verify(DDRemoteDamDesktopSessionService::getInstance);
            services.verifyNoMoreInteractions();
        }
    }

    @Test
    void preservesActivationFailureWhenRollbackAlsoFails() throws Exception {
        var lifecycle = new DDRemoteDamDesktopSessionPluginService();
        var session = Mockito.mock(DDRemoteDamDesktopSessionService.class);
        var failure = new IllegalStateException("Activation failed");
        var cleanupFailure = new IllegalStateException("Cleanup failed");
        Mockito.doThrow(failure).when(session).start();
        Mockito.doThrow(cleanupFailure).when(session).close();
        try (var services = Mockito.mockStatic(DDRemoteDamDesktopSessionService.class)) {
            services.when(DDRemoteDamDesktopSessionService::getInstance).thenReturn(session);

            Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, lifecycle::activateService));
            Assertions.assertArrayEquals(new Throwable[] {cleanupFailure}, failure.getSuppressed());
            Assertions.assertDoesNotThrow(lifecycle::deactivateService);

            Mockito.verify(session).close();
            services.verify(DDRemoteDamDesktopSessionService::getInstance);
            services.verifyNoMoreInteractions();
        }
    }

    @Test
    void shutdownDuringActivationCannotPublishOrRestartAStoppedService() throws Exception {
        var lifecycle = new DDRemoteDamDesktopSessionPluginService();
        var session = Mockito.mock(DDRemoteDamDesktopSessionService.class);
        Mockito.doAnswer(invocation -> {
            lifecycle.deactivateService();
            return null;
        }).when(session).start();
        try (var services = Mockito.mockStatic(DDRemoteDamDesktopSessionService.class)) {
            services.when(DDRemoteDamDesktopSessionService::getInstance).thenReturn(session);

            lifecycle.activateService();
            lifecycle.activateService();
            lifecycle.deactivateService();

            Mockito.verify(session).start();
            Mockito.verify(session).close();
            Mockito.verifyNoMoreInteractions(session);
            services.verify(DDRemoteDamDesktopSessionService::getInstance);
            services.verifyNoMoreInteractions();
        }
    }
}
