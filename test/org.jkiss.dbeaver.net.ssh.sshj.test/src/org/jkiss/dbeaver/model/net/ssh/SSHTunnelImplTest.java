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
package org.jkiss.dbeaver.model.net.ssh;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerDescriptor;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHSessionControllerDescriptor;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHSessionControllerRegistry;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SSHTunnelImplTest extends DBeaverUnitTest {
    @Test
    public void uninitializedTunnelHasNoDependentDataSources() {
        Assertions.assertEquals(0, new SSHTunnelImpl().getDependentDataSources().length);
    }

    @Test
    public void dependenciesAreAvailableOnlyAfterSessionAcquisition() throws Exception {
        SSHTunnelImpl tunnel = new SSHTunnelImpl();
        SSHSessionController controller = Mockito.mock(SSHSessionController.class);
        SSHSession session = Mockito.mock(SSHSession.class);
        SSHSessionControllerRegistry registry = Mockito.mock(SSHSessionControllerRegistry.class);
        SSHSessionControllerDescriptor descriptor = Mockito.mock(SSHSessionControllerDescriptor.class);
        Mockito.when(registry.getDescriptor("test")).thenReturn(descriptor);
        Mockito.when(descriptor.getInstance()).thenReturn(controller);

        DBWHandlerConfiguration configuration = new DBWHandlerConfiguration(Mockito.mock(DBWHandlerDescriptor.class), null);
        configuration.setProperty(SSHConstants.PROP_IMPLEMENTATION, "test");
        configuration.setProperty(DBWHandlerConfiguration.PROP_HOST, "ssh.example.test");
        configuration.setProperty(DBWHandlerConfiguration.PROP_PORT, SSHConstants.DEFAULT_PORT);
        // Explicit ports keep the test independent of network access and local port allocation.
        configuration.setProperty(SSHConstants.PROP_LOCAL_PORT, 12345);
        configuration.setProperty(SSHConstants.PROP_REMOTE_PORT, 5432);
        DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
        connectionInfo.setHostName("database.example.test");
        VoidProgressMonitor monitor = new VoidProgressMonitor();

        Mockito.when(controller.acquireSession(
            Mockito.same(monitor), Mockito.same(configuration), Mockito.any(), Mockito.isNull(), Mockito.any()
        )).thenAnswer(invocation -> {
            // Observe the handler at the exact point where reconnect has a controller but no session yet.
            Assertions.assertSame(controller, tunnel.getController());
            Assertions.assertNull(tunnel.getImplementation());
            Assertions.assertEquals(0, tunnel.getDependentDataSources().length);
            Mockito.verify(controller, Mockito.never()).getDependentDataSources(Mockito.any());
            return session;
        });

        DBPDataSourceContainer[] dependencies = {
            Mockito.mock(DBPDataSourceContainer.class), Mockito.mock(DBPDataSourceContainer.class)
        };
        Mockito.when(controller.getDependentDataSources(session)).thenReturn(dependencies);

        try (var registryMock = Mockito.mockStatic(
            SSHSessionControllerRegistry.class, Mockito.withSettings().mockMaker("mock-maker-inline")
        )) {
            registryMock.when(SSHSessionControllerRegistry::getInstance).thenReturn(registry);
            tunnel.initializeHandler(monitor, configuration, connectionInfo);
        }

        Assertions.assertSame(session, tunnel.getImplementation());
        Assertions.assertArrayEquals(dependencies, tunnel.getDependentDataSources());
        Mockito.verify(controller).getDependentDataSources(session);
    }
}
