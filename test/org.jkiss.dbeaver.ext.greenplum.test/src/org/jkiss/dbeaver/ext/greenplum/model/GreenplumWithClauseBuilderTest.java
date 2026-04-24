/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;
import org.mockito.Mock;

import static org.jkiss.dbeaver.ext.greenplum.model.GreenplumWithClauseBuilder.generateWithClause;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class GreenplumWithClauseBuilderTest extends DBeaverUnitTest {
    @Mock
    private GreenplumTable table;

    @Mock
    private PostgreTableBase tableBase;

    @Mock
    private GreenplumDataSource dataSource;

    @Mock
    private PostgreServerExtension serverExtension;

    @Test
    public void generateWithClause_whenTableWithoutOidsAndWithOptions_shouldDisplayWithClauseWithRelOptions() {

        setupGeneralWhenMocks(false, false);

        when(tableBase.getRelOptions()).thenReturn(new String[]{"appendonly=true"});

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tappendonly=true\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableWithOidsAndOptions_shouldDisplayWithClauseWithOidsAndOptions() {

        setupGeneralWhenMocks(true, true);

        when(tableBase.getRelOptions()).thenReturn(new String[]{"appendonly=true"});

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tOIDS=TRUE,\n\tappendonly=true\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableWithoutOidsAndWithoutOptions_shouldNotDisplayWithClause() {

        setupGeneralWhenMocks(false, false);

        when(tableBase.getRelOptions()).thenReturn(null);

        String withClause = generateWithClause(table, tableBase);
        assertEquals("", withClause);
    }

    @Test
    public void generateWithClause_whenTableWithOidsWithoutOptions_shouldDisplayWithClauseWithOids() {

        setupGeneralWhenMocks(true, true);

        when(tableBase.getRelOptions()).thenReturn(null);

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tOIDS=TRUE\n)", withClause);
    }

    @Test
    public void generateWithClause_whenTableWithOidsWithMultipleOptions_shouldDisplayWithClauseWithOidsAndAllTheOptions() {

        setupGeneralWhenMocks(true, true);

        when(tableBase.getRelOptions()).thenReturn(new String[]{"appendonly=true", "orientation=column"});

        String withClause = generateWithClause(table, tableBase);
        assertEquals("\nWITH (\n\tOIDS=TRUE,\n\tappendonly=true,\n\torientation=column\n)", withClause);
    }

    private void setupGeneralWhenMocks(boolean supportOids, boolean hasOids) {
        when(serverExtension.supportsOids()).thenReturn(supportOids);
        when(dataSource.getServerType()).thenReturn(serverExtension);
        when(table.getDataSource()).thenReturn(dataSource);
        when(table.isHasOids() && dataSource.getServerType().supportsHasOidsColumn()).thenReturn(hasOids);
    }
}
