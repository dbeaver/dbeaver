/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class PostgreServerExtensionBaseTest extends DBeaverUnitTest {

    @Mock
    private PostgreTableRegular table;

    @Mock
    private PostgreDataSource dataSource;

    private PostgreServerPostgreSQL serverExtension;

    @Before
    public void setUp() {
        serverExtension = new PostgreServerPostgreSQL(dataSource);
    }

    @Test
    public void createWithClause_whenOidsAreSupported_shouldDisplayWithClauseWithOidsAsTrue() {
        setupGeneralWhenMocks(true);

        String withClause = serverExtension.createWithClause(table, null);
        assertEquals("\nWITH (\n\tOIDS=TRUE\n)", withClause);
    }

    @Test
    public void createWithClause_whenTableSupportsOidsButDoesNotHaveOids_shouldNotDisplayWithClause() {
        setupGeneralWhenMocks(false);

        String withClause = serverExtension.createWithClause(table, null);
        assertEquals("", withClause);
    }

    @Test
    public void createWithClause_whenTableDoesNotSupportsOids_shouldNotDisplayWithClause() {
        setupGeneralWhenMocks(false);

        String withClause = serverExtension.createWithClause(table, null);
        assertEquals("", withClause);
    }

    private void setupGeneralWhenMocks(boolean hasOids) {
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids()).thenReturn(hasOids);
    }
}
