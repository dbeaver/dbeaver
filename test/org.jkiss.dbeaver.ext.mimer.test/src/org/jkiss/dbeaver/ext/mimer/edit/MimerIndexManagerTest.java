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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndexColumn;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerSQLDialect;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableIndex;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableIndexColumn;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * {@link MimerIndexManager#addObjectCreateActions} - Mimer SQL's {@code CREATE [UNIQUE]
 * CLUSTERED INDEX ...} plus the trailing {@code IGNORE NULLS}/{@code INCLUDE (...)} clauses on
 * top of generic's plain {@code CREATE INDEX}. Quoted identifiers are computed with a
 * real {@link MimerSQLDialect} instance rather than hardcoded, so these tests don't silently rot
 * if the dialect's own quoting rules ever change.
 *
 * @author Mimer Information Technology
 */
public class MimerIndexManagerTest extends DBeaverUnitTest {

    @Mock
    private MimerDataSource dataSource;

    @Mock
    private GenericTableBase table;

    @Mock
    private MimerTableIndex index;

    @Mock
    private DBCExecutionContext executionContext;

    private final MimerIndexManager manager = new MimerIndexManager();
    private final MimerSQLDialect dialect = new MimerSQLDialect();

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(index.getDataSource()).thenReturn(dataSource);
        Mockito.when(index.getTable()).thenReturn(table);
        Mockito.when(index.getName()).thenReturn("cst_email_idx");
        Mockito.when(table.getFullyQualifiedName(DBPEvaluationContext.DDL)).thenReturn("\"mimer_store\".\"customers\"");
        Mockito.when(index.isIgnoreNulls(monitor)).thenReturn(false);
        // Mockito.doReturn(...), not Mockito.when(...).thenReturn(...): getAttributeReferences returns a
        // List<? extends DBSTableIndexColumn> (wildcard), and the column(...) helper does its
        // own nested Mockito.when()-stubbing - both of which trip up the Mockito.when().thenReturn() form.
        Mockito.doReturn(List.of(indexColumn("email", true))).when(index).getAttributeReferences(ArgumentMatchers.any());
        Mockito.doReturn(List.of()).when(index).getIncludedColumns();
    }

    @Test
    public void plainIndexHasNoUniqueClusteredIgnoreNullsOrInclude() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        Mockito.when(index.isUnique()).thenReturn(false);

        String ddl = createDDL();

        Assertions.assertTrue(ddl.startsWith("CREATE INDEX "), ddl);
        Assertions.assertFalse(ddl.contains("UNIQUE"), ddl);
        Assertions.assertFalse(ddl.contains("CLUSTERED"), ddl);
        Assertions.assertFalse(ddl.contains("IGNORE NULLS"), ddl);
        Assertions.assertFalse(ddl.contains("INCLUDE"), ddl);
        Assertions.assertTrue(ddl.contains(" ON \"mimer_store\".\"customers\" ("), ddl);
    }

    @Test
    public void uniqueClusteredIndexIncludesBothModifiersInOrder() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.CLUSTERED);
        Mockito.when(index.isUnique()).thenReturn(true);

        String ddl = createDDL();

        Assertions.assertTrue(ddl.startsWith("CREATE UNIQUE CLUSTERED INDEX "), ddl);
    }

    @Test
    public void ignoreNullsAppearsAfterTheColumnList() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        Mockito.when(index.isUnique()).thenReturn(false);
        Mockito.when(index.isIgnoreNulls(monitor)).thenReturn(true);

        String ddl = createDDL();

        Assertions.assertTrue(ddl.endsWith(") IGNORE NULLS"), ddl);
    }

    /**
     * IGNORE NULLS comes before INCLUDE, not the other way around - explicitly called out in
     * the manager's own source comment, so worth pinning down here.
     */
    @Test
    public void ignoreNullsComesBeforeInclude() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        Mockito.when(index.isUnique()).thenReturn(false);
        Mockito.when(index.isIgnoreNulls(monitor)).thenReturn(true);
        Mockito.doReturn(List.of(tableColumn("balance"))).when(index).getIncludedColumns();

        String ddl = createDDL();

        int ignoreNullsPos = ddl.indexOf("IGNORE NULLS");
        int includePos = ddl.indexOf("INCLUDE");
        Assertions.assertTrue(ignoreNullsPos >= 0 && includePos >= 0 && ignoreNullsPos < includePos, ddl);
        Assertions.assertTrue(ddl.endsWith("INCLUDE (" + dialect.getQuotedIdentifier("balance", true, false) + ")"), ddl);
    }

    @Test
    public void includeListsEveryColumnCommaSeparated() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        Mockito.when(index.isUnique()).thenReturn(false);
        Mockito.doReturn(List.of(tableColumn("balance"), tableColumn("currency"))).when(index).getIncludedColumns();

        String ddl = createDDL();

        String expectedBalance = dialect.getQuotedIdentifier("balance", true, false);
        String expectedCurrency = dialect.getQuotedIdentifier("currency", true, false);
        Assertions.assertTrue(ddl.endsWith("INCLUDE (" + expectedBalance + "," + expectedCurrency + ")"), ddl);
    }

    @Test
    public void multipleIndexColumnsAreCommaSeparatedAndQuoted() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        Mockito.when(index.isUnique()).thenReturn(false);
        Mockito.doReturn(List.of(indexColumn("last_name", true), indexColumn("first_name", true)))
            .when(index).getAttributeReferences(ArgumentMatchers.any());

        String ddl = createDDL();

        String expectedLast = dialect.getQuotedIdentifier("last_name", true, false);
        String expectedFirst = dialect.getQuotedIdentifier("first_name", true, false);
        Assertions.assertTrue(ddl.contains("(" + expectedLast + "," + expectedFirst + ")"), ddl);
    }

    @Test
    public void descendingColumnGetsTheDescModifier() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        Mockito.when(index.isUnique()).thenReturn(false);
        Mockito.doReturn(List.of(indexColumn("created_at", false))).when(index).getAttributeReferences(ArgumentMatchers.any());

        String ddl = createDDL();

        Assertions.assertTrue(ddl.contains(" DESC"), ddl);
    }

    /**
     * Order confirmed by the user: {@code "col" FOR <algorithm> [ASC|DESC]} - the FOR clause
     * comes before, not after, the ASC/DESC suffix.
     */
    @Test
    public void columnAlgorithmAddsAForClauseBeforeTheDescModifier() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        Mockito.when(index.isUnique()).thenReturn(false);
        Mockito.doReturn(List.of(mimerIndexColumn("notes", false, MimerConstants.INDEX_ALGORITHM_WORD_SEARCH)))
            .when(index).getAttributeReferences(ArgumentMatchers.any());

        String ddl = createDDL();

        Assertions.assertTrue(ddl.contains(" FOR WORD_SEARCH DESC"), ddl);
    }

    /**
     * Order confirmed against {@code CREATE_INDEX.htm}: {@code "col" [COLLATE collation] [FOR
     * <algorithm>] [ASC|DESC]} - COLLATE comes right after the column name, before both FOR and
     * ASC/DESC.
     */
    @Test
    public void columnCollationAppearsBeforeAlgorithmAndDescModifier() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        Mockito.when(index.isUnique()).thenReturn(false);
        Mockito.doReturn(List.of(mimerIndexColumn(
            "notes", false, MimerConstants.INDEX_ALGORITHM_WORD_SEARCH, "\"INFORMATION_SCHEMA\".\"ENGLISH_1\"")))
            .when(index).getAttributeReferences(ArgumentMatchers.any());

        String ddl = createDDL();

        int collatePos = ddl.indexOf("COLLATE");
        int forPos = ddl.indexOf("FOR WORD_SEARCH");
        int descPos = ddl.indexOf("DESC");
        Assertions.assertTrue(collatePos >= 0 && forPos > collatePos && descPos > forPos, ddl);
        Assertions.assertTrue(ddl.contains(" COLLATE \"INFORMATION_SCHEMA\".\"ENGLISH_1\" FOR WORD_SEARCH DESC"), ddl);
    }

    @Test
    public void noCollationAddsNoCollateClause() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        Mockito.when(index.isUnique()).thenReturn(false);
        Mockito.doReturn(List.of(mimerIndexColumn("c1", true, null, null))).when(index).getAttributeReferences(ArgumentMatchers.any());

        String ddl = createDDL();

        Assertions.assertFalse(ddl.contains("COLLATE"), ddl);
    }

    @Test
    public void simpleOrMissingAlgorithmAddsNoForClause() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        Mockito.when(index.isUnique()).thenReturn(false);
        Mockito.doReturn(List.of(
            mimerIndexColumn("c1", true, MimerConstants.INDEX_ALGORITHM_SIMPLE),
            mimerIndexColumn("c2", true, null)
        )).when(index).getAttributeReferences(ArgumentMatchers.any());

        String ddl = createDDL();

        Assertions.assertFalse(ddl.contains("FOR"), ddl);
    }

    /**
     * The real bug this guards against: {@link MimerTableIndex#getIncludedColumns()} is
     * create-time-only and always empty for an already-persisted, reloaded index (e.g. after
     * reconnecting), so the owning table's DDL tab used to dump every column - key and
     * INCLUDE-only alike - into the main column list, with no {@code INCLUDE} clause at all.
     * {@link MimerTableIndexColumn#isIncluded} is how a persisted column's real status gets
     * re-checked instead.
     */
    @Test
    public void persistedIndexSeparatesKeyAndIncludeColumnsEvenWithAnEmptyPendingList() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        Mockito.when(index.isUnique()).thenReturn(false);
        DBSTableIndexColumn keyColumn = mimerIndexColumn("i1", true, null);
        DBSTableIndexColumn includedColumn = mimerIndexColumn("i2", true, null);
        Mockito.when(((MimerTableIndexColumn) includedColumn).isIncluded(monitor)).thenReturn(true);
        Mockito.doReturn(List.of(keyColumn, includedColumn)).when(index).getAttributeReferences(ArgumentMatchers.any());
        Mockito.doReturn(List.of()).when(index).getIncludedColumns();

        String ddl = createDDL();

        String expectedI1 = dialect.getQuotedIdentifier("i1", true, false);
        String expectedI2 = dialect.getQuotedIdentifier("i2", true, false);
        Assertions.assertTrue(ddl.contains("(" + expectedI1 + ")"), ddl);
        Assertions.assertFalse(ddl.contains("(" + expectedI1 + "," + expectedI2 + ")"), ddl);
        Assertions.assertTrue(ddl.endsWith("INCLUDE (" + expectedI2 + ")"), ddl);
    }

    /**
     * Belt-and-suspenders re-check at the DDL-building layer (see the manager's class javadoc) -
     * a CLUSTERED index never emits IGNORE NULLS, INCLUDE, or a column FOR clause, regardless of
     * what the model object's own flags say.
     */
    @Test
    public void clusteredIndexNeverEmitsIgnoreNullsIncludeOrAlgorithm() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.CLUSTERED);
        Mockito.when(index.isUnique()).thenReturn(false);
        Mockito.when(index.isIgnoreNulls(monitor)).thenReturn(true);
        Mockito.doReturn(List.of(tableColumn("balance"))).when(index).getIncludedColumns();
        Mockito.doReturn(List.of(mimerIndexColumn("notes", true, MimerConstants.INDEX_ALGORITHM_WORD_SEARCH)))
            .when(index).getAttributeReferences(ArgumentMatchers.any());

        String ddl = createDDL();

        Assertions.assertTrue(ddl.startsWith("CREATE CLUSTERED INDEX "), ddl);
        Assertions.assertFalse(ddl.contains("IGNORE NULLS"), ddl);
        Assertions.assertFalse(ddl.contains("INCLUDE"), ddl);
        Assertions.assertFalse(ddl.contains("FOR"), ddl);
    }

    /**
     * Unlike Ignore Nulls/Include/Algorithm, COLLATE is not excluded on a CLUSTERED index -
     * nothing in the docs suggests it's incompatible with clustering (see the manager's class
     * javadoc).
     */
    @Test
    public void clusteredIndexStillEmitsCollation() throws Exception {
        Mockito.when(index.getIndexType()).thenReturn(DBSIndexType.CLUSTERED);
        Mockito.when(index.isUnique()).thenReturn(false);
        Mockito.doReturn(List.of(mimerIndexColumn("notes", true, null, "\"INFORMATION_SCHEMA\".\"ENGLISH_1\"")))
            .when(index).getAttributeReferences(ArgumentMatchers.any());

        String ddl = createDDL();

        Assertions.assertTrue(ddl.contains(" COLLATE \"INFORMATION_SCHEMA\".\"ENGLISH_1\""), ddl);
    }

    @NotNull
    private String createDDL() throws Exception {
        var command = Mockito.mock(SQLObjectEditor.ObjectCreateCommand.class);
        Mockito.when(command.getObject()).thenReturn(index);

        List<DBEPersistAction> actions = new ArrayList<>();
        manager.addObjectCreateActions(monitor, executionContext, actions, command, Map.of());

        Assertions.assertEquals(1, actions.size());
        return actions.get(0).getScript();
    }

    @NotNull
    private DBSTableIndexColumn indexColumn(@NotNull String name, boolean ascending) {
        GenericTableIndexColumn col = Mockito.mock(GenericTableIndexColumn.class);
        Mockito.when(col.getName()).thenReturn(name);
        Mockito.when(col.getDataSource()).thenReturn(dataSource);
        Mockito.when(col.isAscending()).thenReturn(ascending);
        return col;
    }

    /**
     * Unlike {@link #indexColumn}, mocks the {@link MimerTableIndexColumn} subclass so {@code
     * appendIndexColumnModifiers}' {@code instanceof} check picks up the algorithm/collation -
     * {@code getIndex()} must also be stubbed since the manager's clustered defense re-checks
     * {@code indexColumn.getIndex().getIndexType()} independently of the owning {@link #index}
     * field.
     */
    @NotNull
    private DBSTableIndexColumn mimerIndexColumn(@NotNull String name, boolean ascending, @Nullable String algorithm) {
        return mimerIndexColumn(name, ascending, algorithm, null);
    }

    @NotNull
    private DBSTableIndexColumn mimerIndexColumn(
        @NotNull String name, boolean ascending, @Nullable String algorithm, @Nullable String collation
    ) {
        MimerTableIndexColumn col = Mockito.mock(MimerTableIndexColumn.class);
        Mockito.when(col.getName()).thenReturn(name);
        Mockito.when(col.getDataSource()).thenReturn(dataSource);
        Mockito.when(col.isAscending()).thenReturn(ascending);
        Mockito.when(col.getAlgorithm()).thenReturn(algorithm);
        Mockito.when(col.getCollation()).thenReturn(collation);
        Mockito.doReturn(index).when(col).getIndex();
        return col;
    }

    @NotNull
    private GenericTableColumn tableColumn(@NotNull String name) {
        GenericTableColumn col = Mockito.mock(GenericTableColumn.class);
        Mockito.when(col.getName()).thenReturn(name);
        Mockito.when(col.getDataSource()).thenReturn(dataSource);
        return col;
    }
}
