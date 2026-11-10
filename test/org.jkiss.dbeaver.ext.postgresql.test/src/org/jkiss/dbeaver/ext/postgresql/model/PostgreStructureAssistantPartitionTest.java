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

import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerPostgreSQL;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class PostgreStructureAssistantPartitionTest extends DBeaverUnitTest {

    @Mock
    private PostgreDataSource dataSource;

    @Mock
    private PostgreServerExtension serverType;

    @Test
    public void usesPartitionColumnForModernPostgreSql() {
        when(dataSource.getServerType()).thenReturn(new PostgreServerPostgreSQL(dataSource));
        when(dataSource.isServerVersionAtLeast(10, 0)).thenReturn(true);

        Assertions.assertTrue(PostgreStructureAssistant.supportsPartitionColumn(dataSource));
    }

    @Test
    public void ignoresPartitionColumnWhenServerReportsPartitionsButHasOldVersion() {
        // Greenplum 6 reports partition support but is based on PostgreSQL 9.4,
        // where pg_class.relispartition does not exist yet.
        when(dataSource.getServerType()).thenReturn(serverType);
        when(serverType.supportsPartitions()).thenReturn(true);
        when(dataSource.isServerVersionAtLeast(10, 0)).thenReturn(false);

        Assertions.assertFalse(PostgreStructureAssistant.supportsPartitionColumn(dataSource));
    }

    @Test
    public void ignoresPartitionColumnForServersWithoutPartitionSupport() {
        when(dataSource.getServerType()).thenReturn(serverType);
        when(serverType.supportsPartitions()).thenReturn(false);
        when(dataSource.isServerVersionAtLeast(10, 0)).thenReturn(true);

        Assertions.assertFalse(PostgreStructureAssistant.supportsPartitionColumn(dataSource));
    }
}
