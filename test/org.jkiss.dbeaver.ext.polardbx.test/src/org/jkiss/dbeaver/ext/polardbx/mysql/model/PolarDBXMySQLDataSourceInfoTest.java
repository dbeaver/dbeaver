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
package org.jkiss.dbeaver.ext.polardbx.mysql.model;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class PolarDBXMySQLDataSourceInfoTest extends DBeaverUnitTest {
    @Test
    public void fallsBackToJdbcVersionWhenServerVersionIsEmpty() throws Exception {
        var dataSource = Mockito.mock(PolarDBXMySQLDataSource.class);
        var metadata = Mockito.mock(JDBCDatabaseMetaData.class);
        Mockito.when(dataSource.getServerVersion()).thenReturn("");
        Mockito.when(metadata.getDatabaseProductVersion()).thenReturn("5.7.25");

        var info = new PolarDBXMySQLDataSourceInfo(dataSource, metadata);

        Assertions.assertEquals("5.7.25", info.getDatabaseProductVersion());
    }

    @Test
    public void prefersServerVersionWhenAvailable() throws Exception {
        var dataSource = Mockito.mock(PolarDBXMySQLDataSource.class);
        var metadata = Mockito.mock(JDBCDatabaseMetaData.class);
        Mockito.when(dataSource.getServerVersion()).thenReturn("5.7.25-TDDL-5.4.20-20260909");
        Mockito.when(metadata.getDatabaseProductVersion()).thenReturn("5.7.25");

        var info = new PolarDBXMySQLDataSourceInfo(dataSource, metadata);

        Assertions.assertEquals("5.7.25-TDDL-5.4.20-20260909", info.getDatabaseProductVersion());
    }
}
