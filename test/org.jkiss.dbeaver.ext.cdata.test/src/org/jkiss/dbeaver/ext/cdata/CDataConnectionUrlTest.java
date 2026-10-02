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
package org.jkiss.dbeaver.ext.cdata;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionUrl;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverInfo;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverTier;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerDescriptor;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.net.DBWUtils;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

public class CDataConnectionUrlTest extends DBeaverUnitTest {
    @Test
    public void mapsEffectiveEndpointForSshForwarding() throws Exception {
        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:postgresql:Server=url-host;Port=5432;Database=demo;");
        configuration.getProperties().put("server", "properties-host");
        configuration.getProperties().put("port", "5433");
        CDataConnectionUrl.updateEndpoint("postgresql", configuration, Map.of());
        Assertions.assertEquals("url-host", configuration.getHostName());
        Assertions.assertEquals("5432", configuration.getHostPort());
        Assertions.assertEquals("demo", configuration.getDatabaseName());
    }

    @Test
    public void usesHierarchyDefaultsForOmittedUrlPort() throws Exception {
        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:postgresql:Server=remote;");
        CDataConnectionUrl.updateEndpoint("postgresql", configuration, Map.of("Port", "5432"));
        Assertions.assertEquals("remote", configuration.getHostName());
        Assertions.assertEquals("5432", configuration.getHostPort());
    }

    @Test
    public void forwardsBothConnectionModesWithoutChangingSavedConfiguration() throws Exception {
        for (var mode : DBPDriverConfigurationType.values()) {
            var saved = new DBPConnectionConfiguration();
            saved.setConfigurationType(mode);
            saved.setUrl("jdbc:postgresql:Server=remote;Port=5432;Database=demo;AuthScheme=Password;CustomOption='a;b';");
            saved.getProperties().put("server", "properties-host");
            saved.getProperties().put("port", "5433");
            saved.getProperties().put("Timeout", "90");
            saved.setUserName("test-user");
            saved.setUserPassword("test-password");
            var tunnel = addTunnel(saved);
            CDataConnectionUrl.updateEndpoint("postgresql", saved, Map.of());
            var actual = new DBPConnectionConfiguration(saved);
            DBWUtils.updateConfigWithTunnelInfo(tunnel, actual, "127.0.0.1", 15432);
            var properties = CDataConnectionUrl.parse(actual.getUrl(), "postgresql");
            Assertions.assertEquals("127.0.0.1", properties.get("Server"));
            Assertions.assertEquals("15432", properties.get("Port"));
            Assertions.assertEquals("a;b", properties.get("CustomOption"));
            Assertions.assertEquals("demo", properties.get("Database"));
            Assertions.assertEquals("Password", properties.get("AuthScheme"));
            Assertions.assertEquals(Map.of("server", "127.0.0.1", "port", "15432", "Timeout", "90"), actual.getProperties());
            Assertions.assertEquals("test-password", actual.getUserPassword());
            Assertions.assertFalse(actual.getUrl().contains("test-password"));
            Assertions.assertEquals("remote", saved.getHostName());
            Assertions.assertEquals("5432", saved.getHostPort());
            Assertions.assertEquals("remote", CDataConnectionUrl.parse(saved.getUrl(), "postgresql").get("Server"));
            Assertions.assertEquals("properties-host", saved.getProperties().get("server"));
        }
    }

    @Test
    public void supportsHostPropertyAndIpv6TunnelAddress() throws Exception {
        var configuration = new DBPConnectionConfiguration();
        configuration.setConfigurationType(DBPDriverConfigurationType.URL);
        configuration.setUrl("jdbc:cdata:postgresql:Host=remote;Port=5432;");
        var tunnel = addTunnel(configuration);
        CDataConnectionUrl.updateEndpoint("postgresql", configuration, Map.of());
        Assertions.assertEquals("remote", configuration.getHostName());
        DBWUtils.updateConfigWithTunnelInfo(tunnel, configuration, "::1", 15432);
        Assertions.assertEquals(Map.of("Host", "::1", "Port", "15432"), CDataConnectionUrl.parse(configuration.getUrl(), "postgresql"));
    }

    @Test
    public void disabledTunnelPreservesRawUrlInBothModes() throws Exception {
        var configuration = new DBPConnectionConfiguration();
        String url = "jdbc:cdata:postgresql:Server=remote;CustomOption='a;b';";
        configuration.setUrl(url);
        var tunnel = addTunnel(configuration);
        tunnel.setEnabled(false);
        configuration.setHostName("stale-host");
        configuration.setHostPort("15432");
        for (var mode : DBPDriverConfigurationType.values()) {
            configuration.setConfigurationType(mode);
            Assertions.assertEquals(url, tunnel.getDriver().getConnectionURL(configuration));
        }
    }

    @Test
    public void rejectsTunnelingWithoutServerOrHost() throws Exception {
        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:postgresql:Endpoint=https://example.com;Password=secret;");
        var tunnel = addTunnel(configuration);
        Assertions.assertThrows(DBException.class, () -> DBWUtils.updateConfigWithTunnelInfo(tunnel, configuration, "127.0.0.1", 15432));
    }

    @NotNull
    private DBWHandlerConfiguration addTunnel(@NotNull DBPConnectionConfiguration configuration) {
        var registry = DataSourceProviderRegistry.getInstance();
        var driver = new CDataDriverDescriptor(registry.getDataSourceProvider("generic"), "test-cdata-tunnel",
            new CDataDriverInfo("postgresql", "postgresql", "PostgreSQL JDBC Driver", 2026,
                CDataDriverTier.PROFESSIONAL, "https://www.cdata.com/", "postgresql", null));
        var dataSource = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(dataSource.getDriver()).thenReturn(driver);
        var descriptor = Mockito.mock(DBWHandlerDescriptor.class);
        Mockito.when(descriptor.getId()).thenReturn("ssh_tunnel");
        Mockito.when(descriptor.getType()).thenReturn(DBWHandlerType.TUNNEL);
        var tunnel = new DBWHandlerConfiguration(descriptor, dataSource);
        tunnel.setEnabled(true);
        configuration.updateHandler(tunnel);
        return tunnel;
    }
}
