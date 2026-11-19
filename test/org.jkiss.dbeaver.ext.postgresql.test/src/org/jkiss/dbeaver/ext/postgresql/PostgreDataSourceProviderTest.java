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
package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PostgreDataSourceProviderTest extends DBeaverUnitTest {
    @Test
    void cloudSqlHostIsUsedAsInstanceName() throws DBException {
        DBPDriver driver = configureTestContainer("postgres-gcloud-jdbc").getDriver();
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setConfigurationType(DBPDriverConfigurationType.MANUAL);
        configuration.setHostName("my-project:europe-west4:my-instance");
        configuration.setDatabaseName("postgres");

        Assertions.assertEquals(
            "jdbc:postgresql:///postgres?cloudSqlInstance=my-project:europe-west4:my-instance",
            new PostgreDataSourceProvider().getConnectionURL(driver, configuration)
        );
    }
}
