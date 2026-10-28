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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

public class PostgreDataSourceSSLNegotiationTest extends DBeaverUnitTest {

    @Mock
    private DBWHandlerConfiguration sslConfig;

    @Test
    public void initSSLNegotiationWhenDirectShouldPassDriverProperty() {
        when(sslConfig.getStringProperty(PostgreConstants.PROP_SSL_NEGOTIATION))
            .thenReturn(PostgreConstants.SSL_NEGOTIATION_DIRECT);
        Map<String, String> props = new HashMap<>();

        PostgreDataSource.initSSLNegotiation(props, sslConfig);

        Assertions.assertEquals(PostgreConstants.SSL_NEGOTIATION_DIRECT, props.get(PostgreConstants.DRIVER_PROP_SSL_NEGOTIATION));
    }

    @Test
    public void initSSLNegotiationWhenPostgresShouldPassDriverProperty() {
        when(sslConfig.getStringProperty(PostgreConstants.PROP_SSL_NEGOTIATION))
            .thenReturn(PostgreConstants.SSL_NEGOTIATION_POSTGRES);
        Map<String, String> props = new HashMap<>();

        PostgreDataSource.initSSLNegotiation(props, sslConfig);

        Assertions.assertEquals(PostgreConstants.SSL_NEGOTIATION_POSTGRES, props.get(PostgreConstants.DRIVER_PROP_SSL_NEGOTIATION));
    }

    @Test
    public void initSSLNegotiationWhenEmptyShouldKeepDriverDefault() {
        when(sslConfig.getStringProperty(PostgreConstants.PROP_SSL_NEGOTIATION)).thenReturn("");
        Map<String, String> props = new HashMap<>();

        PostgreDataSource.initSSLNegotiation(props, sslConfig);

        Assertions.assertFalse(props.containsKey(PostgreConstants.DRIVER_PROP_SSL_NEGOTIATION));
    }

    @Test
    public void initSSLNegotiationWhenNotSetShouldKeepDriverDefault() {
        Map<String, String> props = new HashMap<>();

        PostgreDataSource.initSSLNegotiation(props, sslConfig);

        Assertions.assertFalse(props.containsKey(PostgreConstants.DRIVER_PROP_SSL_NEGOTIATION));
    }
}
