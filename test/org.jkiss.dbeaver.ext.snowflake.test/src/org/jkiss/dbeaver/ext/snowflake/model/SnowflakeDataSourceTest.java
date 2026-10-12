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
package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.dbeaver.ext.snowflake.SnowflakeConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnowflakeDataSourceTest {
    @Test
    void internalConnectionPropertiesContainWarehouse() {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setServerName("test_warehouse");

        assertEquals(
            "test_warehouse",
            SnowflakeDataSource.getInternalConnectionProperties(configuration).get(SnowflakeConstants.PROP_WAREHOUSE));
    }

    @Test
    void internalConnectionPropertiesContainLegacyWarehouse() {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setProviderProperty(SnowflakeConstants.PROP_WAREHOUSE, "legacy_warehouse");

        assertEquals(
            "legacy_warehouse",
            SnowflakeDataSource.getInternalConnectionProperties(configuration).get(SnowflakeConstants.PROP_WAREHOUSE));
    }
}
