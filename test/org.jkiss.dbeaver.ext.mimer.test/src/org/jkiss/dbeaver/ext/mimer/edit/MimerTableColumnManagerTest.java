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
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerSQLDialect;
import org.jkiss.dbeaver.ext.mimer.model.MimerSequence;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableColumn;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;


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
        Mockito.when(column.getParentObject()).thenReturn(table);
        Mockito.when(column.getTable()).thenReturn(table);
        Mockito.when(column.getName()).thenReturn("id");
        Mockito.when(column.getDataSource()).thenReturn(dataSource);
        Mockito.when(table.getSchema()).thenReturn(schema);
        Mockito.when(table.getName()).thenReturn("customers");
        Mockito.when(schema.getName()).thenReturn("mimer_store");
        Mockito.when(schema.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new MimerSQLDialect());
        Mockito.when(dataSource.supportsNextValueForSyntax()).thenReturn(true);
        Mockito.when(dataSource.supportsModernSequenceSyntax()).thenReturn(true);
    }

    @Test
    public void nonAutoIncrementColumnIsANoOp() throws Exception {
        Mockito.when(column.isAutoIncrement()).thenReturn(false);

        List<DBEPersistAction> actions = MimerTableColumnManager.prepareAutoIncrementColumn(monitor, column);

        Assertions.assertTrue(actions.isEmpty());
        Mockito.verify(column, Mockito.never()).setDefaultValue(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    public void blankSequenceNameDefaultsToTableUnderscoreColumnUnderscoreSeq() throws Exception {
        Mockito.when(column.isAutoIncrement()).thenReturn(true);
        Mockito.when(column.getSequenceName()).thenReturn("");
        org.mockito.Mockito.doReturn(List.of()).when(schema).getSequences(monitor);

        List<DBEPersistAction> actions = MimerTableColumnManager.prepareAutoIncrementColumn(monitor, column);

        Mockito.verify(column).setDefaultValue("NEXT VALUE FOR \"mimer_store\".\"customers_id_seq\"");
        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals(
            new MimerSequence(schema, "customers_id_seq").buildCreateDDL(),
            actions.get(0).getScript());
    }

    @Test
    public void anExplicitSequenceNameIsUsedVerbatim() throws Exception {
        Mockito.when(column.isAutoIncrement()).thenReturn(true);
        Mockito.when(column.getSequenceName()).thenReturn("  my_custom_seq  "); // must be trimmed
        org.mockito.Mockito.doReturn(List.of()).when(schema).getSequences(monitor);

        List<DBEPersistAction> actions = MimerTableColumnManager.prepareAutoIncrementColumn(monitor, column);

        Mockito.verify(column).setDefaultValue("NEXT VALUE FOR \"mimer_store\".\"my_custom_seq\"");
        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals(
            new MimerSequence(schema, "my_custom_seq").buildCreateDDL(),
            actions.get(0).getScript());
    }

    @Test
    public void reusesAnAlreadyExistingSequenceInsteadOfCreatingANewOne() throws Exception {
        Mockito.when(column.isAutoIncrement()).thenReturn(true);
        Mockito.when(column.getSequenceName()).thenReturn("customers_id_seq");
        GenericSequence existing = Mockito.mock(GenericSequence.class);
        Mockito.when(existing.getName()).thenReturn("CUSTOMERS_ID_SEQ"); // case-insensitive match
        org.mockito.Mockito.doReturn(List.of(existing)).when(schema).getSequences(monitor);

        List<DBEPersistAction> actions = MimerTableColumnManager.prepareAutoIncrementColumn(monitor, column);

        // The default value is still pointed at the sequence even when reused - only the
        // CREATE SEQUENCE action itself is skipped.
        Mockito.verify(column).setDefaultValue("NEXT VALUE FOR \"mimer_store\".\"customers_id_seq\"");
        Assertions.assertTrue(actions.isEmpty());
    }

    @Test
    public void usesTheLegacyNextValueOfSyntaxOnAnOlderServer() throws Exception {
        Mockito.when(dataSource.supportsNextValueForSyntax()).thenReturn(false);
        Mockito.when(column.isAutoIncrement()).thenReturn(true);
        Mockito.when(column.getSequenceName()).thenReturn("");
        org.mockito.Mockito.doReturn(List.of()).when(schema).getSequences(monitor);

        MimerTableColumnManager.prepareAutoIncrementColumn(monitor, column);

        Mockito.verify(column).setDefaultValue("NEXT_VALUE OF \"mimer_store\".\"customers_id_seq\"");
    }

    /**
     * {@link MimerTableColumnManager#isPrimaryKeyColumn} - reported live: ticking only "Primary
     * Key" (not "Not Null") for a new column still produced an explicit {@code NOT NULL} in the
     * generated DDL, even though a plain hand-written {@code "col TYPE PRIMARY KEY"} never
     * needs one.
     */
    @Test
    public void columnReferencedByThePrimaryKeyIsRecognised() throws Exception {
        GenericUniqueKey primaryKey = Mockito.mock(GenericUniqueKey.class);
        Mockito.when(primaryKey.getConstraintType()).thenReturn(DBSEntityConstraintType.PRIMARY_KEY);
        DBSEntityAttributeRef ref = Mockito.mock(DBSEntityAttributeRef.class);
        Mockito.doReturn(column).when(ref).getAttribute();
        Mockito.doReturn(List.of(ref)).when(primaryKey).getAttributeReferences(monitor);
        Mockito.doReturn(List.of(primaryKey)).when(table).getConstraints(monitor);

        Assertions.assertTrue(MimerTableColumnManager.isPrimaryKeyColumn(monitor, column));
    }

    @Test
    public void columnNotReferencedByAnyPrimaryKeyIsNotRecognised() throws Exception {
        GenericUniqueKey primaryKey = Mockito.mock(GenericUniqueKey.class);
        Mockito.when(primaryKey.getConstraintType()).thenReturn(DBSEntityConstraintType.PRIMARY_KEY);
        MimerTableColumn otherColumn = Mockito.mock(MimerTableColumn.class);
        DBSEntityAttributeRef ref = Mockito.mock(DBSEntityAttributeRef.class);
        Mockito.doReturn(otherColumn).when(ref).getAttribute();
        Mockito.doReturn(List.of(ref)).when(primaryKey).getAttributeReferences(monitor);
        Mockito.doReturn(List.of(primaryKey)).when(table).getConstraints(monitor);

        Assertions.assertFalse(MimerTableColumnManager.isPrimaryKeyColumn(monitor, column));
    }

    @Test
    public void noConstraintsAtAllMeansNotAPrimaryKeyColumn() throws Exception {
        Mockito.doReturn(List.of()).when(table).getConstraints(monitor);

        Assertions.assertFalse(MimerTableColumnManager.isPrimaryKeyColumn(monitor, column));
    }

    /**
     * {@link MimerTableColumnManager#isRedundantNotNull} - the full decision. A persisted
     * column is always redundant regardless of primary-key membership, since Mimer SQL already
     * backs an explicit {@code NOT NULL} with its own synthetic {@code CHECK} constraint that
     * the table's own DDL reconstruction lists separately - this is the regression test for the
     * compounding-constraint bug found live: a reconstructed DDL that still showed the inline
     * keyword, once copied back in as a literal {@code CREATE TABLE}, made Mimer SQL
     * auto-generate yet another synthetic {@code CHECK} on top of the one being copied.
     */
    @Test
    public void persistedRequiredColumnIsRedundantEvenWithNoPrimaryKeyAtAll() throws Exception {
        Mockito.when(column.isRequired()).thenReturn(true);
        Mockito.when(column.isPersisted()).thenReturn(true);

        Assertions.assertTrue(MimerTableColumnManager.isRedundantNotNull(monitor, column));
        Mockito.verify(table, Mockito.never()).getConstraints(ArgumentMatchers.any());
    }

    @Test
    public void notYetPersistedPrimaryKeyColumnIsRedundant() throws Exception {
        Mockito.when(column.isRequired()).thenReturn(true);
        Mockito.when(column.isPersisted()).thenReturn(false);
        GenericUniqueKey primaryKey = Mockito.mock(GenericUniqueKey.class);
        Mockito.when(primaryKey.getConstraintType()).thenReturn(DBSEntityConstraintType.PRIMARY_KEY);
        DBSEntityAttributeRef ref = Mockito.mock(DBSEntityAttributeRef.class);
        Mockito.doReturn(column).when(ref).getAttribute();
        Mockito.doReturn(List.of(ref)).when(primaryKey).getAttributeReferences(monitor);
        Mockito.doReturn(List.of(primaryKey)).when(table).getConstraints(monitor);

        Assertions.assertTrue(MimerTableColumnManager.isRedundantNotNull(monitor, column));
    }

    /**
     * The ordinary "add a new, non-key column and tick Not Null" case must keep working - the
     * keyword is still the real mechanism for creating that behavior on a not-yet-persisted
     * column.
     */
    @Test
    public void notYetPersistedNonPrimaryKeyColumnIsNotRedundant() throws Exception {
        Mockito.when(column.isRequired()).thenReturn(true);
        Mockito.when(column.isPersisted()).thenReturn(false);
        Mockito.doReturn(List.of()).when(table).getConstraints(monitor);

        Assertions.assertFalse(MimerTableColumnManager.isRedundantNotNull(monitor, column));
    }

    @Test
    public void notRequiredColumnIsNeverRedundantRegardlessOfConstraints() throws Exception {
        Mockito.when(column.isRequired()).thenReturn(false);

        Assertions.assertFalse(MimerTableColumnManager.isRedundantNotNull(monitor, column));
        Mockito.verify(table, Mockito.never()).getConstraints(ArgumentMatchers.any());
    }
}
