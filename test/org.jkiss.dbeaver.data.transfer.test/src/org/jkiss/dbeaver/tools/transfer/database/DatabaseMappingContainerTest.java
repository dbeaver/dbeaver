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
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DatabaseMappingContainerTest {

    @Test
    public void quotedSourceNameRequiresCasePreservation() {
        DBSDataContainer source = (DBSDataContainer) Mockito.mock(
            DBSTable.class,
            Mockito.withSettings().extraInterfaces(DBSDataContainer.class)
        );
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(source.getName()).thenReturn("version_A22");
        Mockito.when(source.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.mustBeQuoted("version_A22", true)).thenReturn(true);

        DatabaseMappingContainer container = new DatabaseMappingContainer(
            Mockito.mock(DatabaseConsumerSettings.class),
            source
        );

        Assertions.assertTrue(container.isSourceNameQuoted());
    }

    @Test
    public void unquotedSourceNameDoesNotRequireCasePreservation() {
        DBSDataContainer source = (DBSDataContainer) Mockito.mock(
            DBSTable.class,
            Mockito.withSettings().extraInterfaces(DBSDataContainer.class)
        );
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(source.getName()).thenReturn("version_a22");
        Mockito.when(source.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.mustBeQuoted("version_a22", true)).thenReturn(false);

        DatabaseMappingContainer container = new DatabaseMappingContainer(
            Mockito.mock(DatabaseConsumerSettings.class),
            source
        );

        Assertions.assertFalse(container.isSourceNameQuoted());
    }
}
