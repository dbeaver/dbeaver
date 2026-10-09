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
package org.jkiss.dbeaver.ext.mimer.edit;

import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerSQLDialect;
import org.jkiss.dbeaver.ext.mimer.model.MimerSequence;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableColumn;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MimerTableColumnManager#prepareAutoIncrementColumn} - the shared "Auto Generated"
 * checkbox logic ({@link MimerTableManager} calls the same static method for the {@code CREATE
 * TABLE} path). A package-visible
 * static method, directly callable here with no OSGi-mock-across-bundles problem (unlike the
 * `addObjectModifyActions`-family methods - see {@code MimerSchemaManagerTest}'s Javadoc).
 *
 * @author Mimer Information Technology
 */
public class MimerTableColumnManagerTest extends DBeaverUnitTest {

    @Mock
    private MimerTableColumn column;

    @Mock
    private GenericTableBase table;

    @Mock
    private GenericSchema schema;

    @Mock
    private MimerDataSource dataSource;

    @BeforeEach
    public void setUp() {
        when(column.getParentObject()).thenReturn(table);
        when(column.getName()).thenReturn("id");
        when(column.getDataSource()).thenReturn(dataSource);
        when(table.getSchema()).thenReturn(schema);
        when(table.getName()).thenReturn("customers");
        when(schema.getName()).thenReturn("mimer_store");
        when(schema.getDataSource()).thenReturn(dataSource);
        when(dataSource.getSQLDialect()).thenReturn(new MimerSQLDialect());
        when(dataSource.supportsNextValueForSyntax()).thenReturn(true);
        when(dataSource.supportsModernSequenceSyntax()).thenReturn(true);
    }

    @Test
    public void nonAutoIncrementColumnIsANoOp() throws Exception {
        when(column.isAutoIncrement()).thenReturn(false);

        List<DBEPersistAction> actions = MimerTableColumnManager.prepareAutoIncrementColumn(monitor, column);

        Assertions.assertTrue(actions.isEmpty());
        verify(column, never()).setDefaultValue(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    public void blankSequenceNameDefaultsToTableUnderscoreColumnUnderscoreSeq() throws Exception {
        when(column.isAutoIncrement()).thenReturn(true);
        when(column.getSequenceName()).thenReturn("");
        org.mockito.Mockito.doReturn(List.of()).when(schema).getSequences(monitor);

        List<DBEPersistAction> actions = MimerTableColumnManager.prepareAutoIncrementColumn(monitor, column);

        verify(column).setDefaultValue("NEXT VALUE FOR \"mimer_store\".\"customers_id_seq\"");
        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals(
            new MimerSequence(schema, "customers_id_seq").buildCreateDDL(),
            actions.get(0).getScript());
    }

    @Test
    public void anExplicitSequenceNameIsUsedVerbatim() throws Exception {
        when(column.isAutoIncrement()).thenReturn(true);
        when(column.getSequenceName()).thenReturn("  my_custom_seq  "); // must be trimmed
        org.mockito.Mockito.doReturn(List.of()).when(schema).getSequences(monitor);

        List<DBEPersistAction> actions = MimerTableColumnManager.prepareAutoIncrementColumn(monitor, column);

        verify(column).setDefaultValue("NEXT VALUE FOR \"mimer_store\".\"my_custom_seq\"");
        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals(
            new MimerSequence(schema, "my_custom_seq").buildCreateDDL(),
            actions.get(0).getScript());
    }

    @Test
    public void reusesAnAlreadyExistingSequenceInsteadOfCreatingANewOne() throws Exception {
        when(column.isAutoIncrement()).thenReturn(true);
        when(column.getSequenceName()).thenReturn("customers_id_seq");
        GenericSequence existing = mock(GenericSequence.class);
        when(existing.getName()).thenReturn("CUSTOMERS_ID_SEQ"); // case-insensitive match
        org.mockito.Mockito.doReturn(List.of(existing)).when(schema).getSequences(monitor);

        List<DBEPersistAction> actions = MimerTableColumnManager.prepareAutoIncrementColumn(monitor, column);

        // The default value is still pointed at the sequence even when reused - only the
        // CREATE SEQUENCE action itself is skipped.
        verify(column).setDefaultValue("NEXT VALUE FOR \"mimer_store\".\"customers_id_seq\"");
        Assertions.assertTrue(actions.isEmpty());
    }

    @Test
    public void usesTheLegacyNextValueOfSyntaxOnAnOlderServer() throws Exception {
        when(dataSource.supportsNextValueForSyntax()).thenReturn(false);
        when(column.isAutoIncrement()).thenReturn(true);
        when(column.getSequenceName()).thenReturn("");
        org.mockito.Mockito.doReturn(List.of()).when(schema).getSequences(monitor);

        MimerTableColumnManager.prepareAutoIncrementColumn(monitor, column);

        verify(column).setDefaultValue("NEXT_VALUE OF \"mimer_store\".\"customers_id_seq\"");
    }
}
