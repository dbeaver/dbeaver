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

class DDDesktopConnectionPluginServiceTest extends DBeaverUnitTest {
    @Test
    void activatesAndClosesTheRetainedServiceOnlyOnce() throws Exception {
        var lifecycle = new DDDesktopConnectionPluginService();
        var connection = Mockito.mock(DDDesktopConnectionService.class);
        try (var services = Mockito.mockStatic(DDDesktopConnectionService.class)) {
            services.when(DDDesktopConnectionService::getInstance).thenReturn(connection);

            lifecycle.activateService();
            lifecycle.activateService();
            lifecycle.deactivateService();
            lifecycle.deactivateService();
            lifecycle.activateService();

            var order = Mockito.inOrder(connection);
            order.verify(connection).start();
            order.verify(connection).close();
            Mockito.verifyNoMoreInteractions(connection);
            services.verify(DDDesktopConnectionService::getInstance);
            services.verifyNoMoreInteractions();
        }
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void shutdownWithoutActivationNeverCreatesAConnectionService() {
        var lifecycle = new DDDesktopConnectionPluginService();
        try (var services = Mockito.mockStatic(DDDesktopConnectionService.class)) {
            lifecycle.deactivateService();
            lifecycle.deactivateService();
            lifecycle.activateService();

            services.verifyNoInteractions();
        }
    }

    @Test
    void initializationFailureDoesNotCreateAServiceDuringShutdown() throws Exception {
        var lifecycle = new DDDesktopConnectionPluginService();
        try (var services = Mockito.mockStatic(DDDesktopConnectionService.class)) {
            services.when(DDDesktopConnectionService::getInstance).thenThrow(new DBException("Initialization failed"));

            Assertions.assertDoesNotThrow(lifecycle::activateService);
            lifecycle.deactivateService();
            lifecycle.activateService();

            services.verify(DDDesktopConnectionService::getInstance);
            services.verifyNoMoreInteractions();
        }
    }

    @Test
    void rollsBackPartialActivationWithoutLeavingAServiceForShutdown() throws Exception {
        var lifecycle = new DDDesktopConnectionPluginService();
        var connection = Mockito.mock(DDDesktopConnectionService.class);
        var failure = new IllegalStateException("Activation failed");
        Mockito.doThrow(failure).when(connection).start();
        try (var services = Mockito.mockStatic(DDDesktopConnectionService.class)) {
            services.when(DDDesktopConnectionService::getInstance).thenReturn(connection);

            Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, lifecycle::activateService));
            lifecycle.deactivateService();
            lifecycle.activateService();

            Mockito.verify(connection).start();
            Mockito.verify(connection).close();
            Mockito.verifyNoMoreInteractions(connection);
            services.verify(DDDesktopConnectionService::getInstance);
            services.verifyNoMoreInteractions();
        }
    }

    @Test
    void preservesActivationFailureWhenRollbackAlsoFails() throws Exception {
        var lifecycle = new DDDesktopConnectionPluginService();
        var connection = Mockito.mock(DDDesktopConnectionService.class);
        var failure = new IllegalStateException("Activation failed");
        var cleanupFailure = new IllegalStateException("Cleanup failed");
        Mockito.doThrow(failure).when(connection).start();
        Mockito.doThrow(cleanupFailure).when(connection).close();
        try (var services = Mockito.mockStatic(DDDesktopConnectionService.class)) {
            services.when(DDDesktopConnectionService::getInstance).thenReturn(connection);

            Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, lifecycle::activateService));
            Assertions.assertArrayEquals(new Throwable[] {cleanupFailure}, failure.getSuppressed());
            Assertions.assertDoesNotThrow(lifecycle::deactivateService);

            Mockito.verify(connection).close();
            services.verify(DDDesktopConnectionService::getInstance);
            services.verifyNoMoreInteractions();
        }
    }

    @Test
    void shutdownDuringActivationCannotPublishOrRestartAStoppedService() throws Exception {
        var lifecycle = new DDDesktopConnectionPluginService();
        var connection = Mockito.mock(DDDesktopConnectionService.class);
        Mockito.doAnswer(invocation -> {
            lifecycle.deactivateService();
            return null;
        }).when(connection).start();
        try (var services = Mockito.mockStatic(DDDesktopConnectionService.class)) {
            services.when(DDDesktopConnectionService::getInstance).thenReturn(connection);

            lifecycle.activateService();
            lifecycle.activateService();
            lifecycle.deactivateService();

            Mockito.verify(connection).start();
            Mockito.verify(connection).close();
            Mockito.verifyNoMoreInteractions(connection);
            services.verify(DDDesktopConnectionService::getInstance);
            services.verifyNoMoreInteractions();
        }
    }
}
