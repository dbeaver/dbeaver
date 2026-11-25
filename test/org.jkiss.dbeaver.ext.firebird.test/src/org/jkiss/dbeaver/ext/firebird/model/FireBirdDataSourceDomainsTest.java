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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class FireBirdDataSourceDomainsTest extends DBeaverUnitTest {

    private FireBirdDataSource dataSource;
    private DBRProgressMonitor monitor;

    @BeforeEach
    public void setUp() throws DBException {
        dataSource = Mockito.mock(FireBirdDataSource.class);
        monitor = Mockito.mock(DBRProgressMonitor.class);
        Mockito.doReturn(List.of(
            domain("DOM_UUID", false),
            // Implicit domain of a column: not flagged as system, but internal
            domain("RDB$100", false),
            domain("RDB$FIELD_NAME", true),
            domain("MON$IDLE_TIMER", true),
            // User domains may contain '$'
            domain("DOM$USD", false),
            new FireBirdDataType(dataSource, FireBirdFieldType.INTEGER),
            new FireBirdDataType(dataSource, FireBirdFieldType.VARCHAR),
            domain("DOM_AMOUNT", false)
        )).when(dataSource).getDataTypes(monitor);
        Mockito.when(dataSource.getDomains(monitor)).thenCallRealMethod();
        Mockito.when(dataSource.getBuiltInDataTypes(monitor)).thenCallRealMethod();
    }

    @Test
    public void domainsContainOnlyUserDefinedDomains() throws DBException {
        Assertions.assertEquals(
            List.of("DOM_UUID", "DOM$USD", "DOM_AMOUNT"),
            dataSource.getDomains(monitor).stream().map(FireBirdDataType::getTypeName).toList());
    }

    @Test
    public void builtInDataTypesExcludeDomains() throws DBException {
        Assertions.assertEquals(
            List.of(FireBirdFieldType.INTEGER.getName(), FireBirdFieldType.VARCHAR.getName()),
            dataSource.getBuiltInDataTypes(monitor).stream().map(FireBirdDataType::getTypeName).toList());
    }

    @NotNull
    private FireBirdDataType domain(@NotNull String name, boolean system) {
        return new FireBirdDataType(
            dataSource, FireBirdFieldType.VARCHAR, 0, name, null, false, true, 0, 0, 0,
            36, 36, null, null, null, "UTF8", false, system);
    }
}
