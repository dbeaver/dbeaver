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
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        when(dataSource.getSQLDialect()).thenReturn(dialect);
        when(index.getDataSource()).thenReturn(dataSource);
        when(index.getTable()).thenReturn(table);
        when(index.getName()).thenReturn("cst_email_idx");
        when(table.getFullyQualifiedName(DBPEvaluationContext.DDL)).thenReturn("\"mimer_store\".\"customers\"");
        when(index.isIgnoreNulls(monitor)).thenReturn(false);
        // doReturn(...), not when(...).thenReturn(...): getAttributeReferences returns a
        // List<? extends DBSTableIndexColumn> (wildcard), and the column(...) helper does its
        // own nested when()-stubbing - both of which trip up the when().thenReturn() form.
        doReturn(List.of(indexColumn("email", true))).when(index).getAttributeReferences(any());
        doReturn(List.of()).when(index).getIncludedColumns();
    }

    @Test
    public void plainIndexHasNoUniqueClusteredIgnoreNullsOrInclude() throws Exception {
        when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        when(index.isUnique()).thenReturn(false);

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
        when(index.getIndexType()).thenReturn(DBSIndexType.CLUSTERED);
        when(index.isUnique()).thenReturn(true);

        String ddl = createDDL();

        Assertions.assertTrue(ddl.startsWith("CREATE UNIQUE CLUSTERED INDEX "), ddl);
    }

    @Test
    public void ignoreNullsAppearsAfterTheColumnList() throws Exception {
        when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        when(index.isUnique()).thenReturn(false);
        when(index.isIgnoreNulls(monitor)).thenReturn(true);

        String ddl = createDDL();

        Assertions.assertTrue(ddl.endsWith(") IGNORE NULLS"), ddl);
    }

    /**
     * IGNORE NULLS comes before INCLUDE, not the other way around - explicitly called out in
     * the manager's own source comment, so worth pinning down here.
     */
    @Test
    public void ignoreNullsComesBeforeInclude() throws Exception {
        when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        when(index.isUnique()).thenReturn(false);
        when(index.isIgnoreNulls(monitor)).thenReturn(true);
        doReturn(List.of(tableColumn("balance"))).when(index).getIncludedColumns();

        String ddl = createDDL();

        int ignoreNullsPos = ddl.indexOf("IGNORE NULLS");
        int includePos = ddl.indexOf("INCLUDE");
        Assertions.assertTrue(ignoreNullsPos >= 0 && includePos >= 0 && ignoreNullsPos < includePos, ddl);
        Assertions.assertTrue(ddl.endsWith("INCLUDE (" + dialect.getQuotedIdentifier("balance", true, false) + ")"), ddl);
    }

    @Test
    public void includeListsEveryColumnCommaSeparated() throws Exception {
        when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        when(index.isUnique()).thenReturn(false);
        doReturn(List.of(tableColumn("balance"), tableColumn("currency"))).when(index).getIncludedColumns();

        String ddl = createDDL();

        String expectedBalance = dialect.getQuotedIdentifier("balance", true, false);
        String expectedCurrency = dialect.getQuotedIdentifier("currency", true, false);
        Assertions.assertTrue(ddl.endsWith("INCLUDE (" + expectedBalance + "," + expectedCurrency + ")"), ddl);
    }

    @Test
    public void multipleIndexColumnsAreCommaSeparatedAndQuoted() throws Exception {
        when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        when(index.isUnique()).thenReturn(false);
        doReturn(List.of(indexColumn("last_name", true), indexColumn("first_name", true)))
            .when(index).getAttributeReferences(any());

        String ddl = createDDL();

        String expectedLast = dialect.getQuotedIdentifier("last_name", true, false);
        String expectedFirst = dialect.getQuotedIdentifier("first_name", true, false);
        Assertions.assertTrue(ddl.contains("(" + expectedLast + "," + expectedFirst + ")"), ddl);
    }

    @Test
    public void aDescendingColumnGetsTheDescModifier() throws Exception {
        when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        when(index.isUnique()).thenReturn(false);
        doReturn(List.of(indexColumn("created_at", false))).when(index).getAttributeReferences(any());

        String ddl = createDDL();

        Assertions.assertTrue(ddl.contains(" DESC"), ddl);
    }

    /**
     * Order confirmed by the user: {@code "col" FOR <algorithm> [ASC|DESC]} - the FOR clause
     * comes before, not after, the ASC/DESC suffix.
     */
    @Test
    public void columnAlgorithmAddsAForClauseBeforeTheDescModifier() throws Exception {
        when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        when(index.isUnique()).thenReturn(false);
        doReturn(List.of(mimerIndexColumn("notes", false, MimerConstants.INDEX_ALGORITHM_WORD_SEARCH)))
            .when(index).getAttributeReferences(any());

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
        when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        when(index.isUnique()).thenReturn(false);
        doReturn(List.of(mimerIndexColumn("notes", false, MimerConstants.INDEX_ALGORITHM_WORD_SEARCH, "\"INFORMATION_SCHEMA\".\"ENGLISH_1\"")))
            .when(index).getAttributeReferences(any());

        String ddl = createDDL();

        int collatePos = ddl.indexOf("COLLATE");
        int forPos = ddl.indexOf("FOR WORD_SEARCH");
        int descPos = ddl.indexOf("DESC");
        Assertions.assertTrue(collatePos >= 0 && forPos > collatePos && descPos > forPos, ddl);
        Assertions.assertTrue(ddl.contains(" COLLATE \"INFORMATION_SCHEMA\".\"ENGLISH_1\" FOR WORD_SEARCH DESC"), ddl);
    }

    @Test
    public void noCollationAddsNoCollateClause() throws Exception {
        when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        when(index.isUnique()).thenReturn(false);
        doReturn(List.of(mimerIndexColumn("c1", true, null, null))).when(index).getAttributeReferences(any());

        String ddl = createDDL();

        Assertions.assertFalse(ddl.contains("COLLATE"), ddl);
    }

    @Test
    public void simpleOrMissingAlgorithmAddsNoForClause() throws Exception {
        when(index.getIndexType()).thenReturn(DBSIndexType.OTHER);
        when(index.isUnique()).thenReturn(false);
        doReturn(List.of(
            mimerIndexColumn("c1", true, MimerConstants.INDEX_ALGORITHM_SIMPLE),
            mimerIndexColumn("c2", true, null)
        )).when(index).getAttributeReferences(any());

        String ddl = createDDL();

        Assertions.assertFalse(ddl.contains("FOR"), ddl);
    }

    /**
     * Belt-and-suspenders re-check at the DDL-building layer (see the manager's class javadoc) -
     * a CLUSTERED index never emits IGNORE NULLS, INCLUDE, or a column FOR clause, regardless of
     * what the model object's own flags say.
     */
    @Test
    public void clusteredIndexNeverEmitsIgnoreNullsIncludeOrAlgorithm() throws Exception {
        when(index.getIndexType()).thenReturn(DBSIndexType.CLUSTERED);
        when(index.isUnique()).thenReturn(false);
        when(index.isIgnoreNulls(monitor)).thenReturn(true);
        doReturn(List.of(tableColumn("balance"))).when(index).getIncludedColumns();
        doReturn(List.of(mimerIndexColumn("notes", true, MimerConstants.INDEX_ALGORITHM_WORD_SEARCH)))
            .when(index).getAttributeReferences(any());

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
        when(index.getIndexType()).thenReturn(DBSIndexType.CLUSTERED);
        when(index.isUnique()).thenReturn(false);
        doReturn(List.of(mimerIndexColumn("notes", true, null, "\"INFORMATION_SCHEMA\".\"ENGLISH_1\"")))
            .when(index).getAttributeReferences(any());

        String ddl = createDDL();

        Assertions.assertTrue(ddl.contains(" COLLATE \"INFORMATION_SCHEMA\".\"ENGLISH_1\""), ddl);
    }

    private String createDDL() throws Exception {
        var command = mock(SQLObjectEditor.ObjectCreateCommand.class);
        when(command.getObject()).thenReturn(index);

        List<DBEPersistAction> actions = new ArrayList<>();
        manager.addObjectCreateActions(monitor, executionContext, actions, command, Map.of());

        Assertions.assertEquals(1, actions.size());
        return actions.get(0).getScript();
    }

    private DBSTableIndexColumn indexColumn(String name, boolean ascending) {
        GenericTableIndexColumn col = mock(GenericTableIndexColumn.class);
        when(col.getName()).thenReturn(name);
        when(col.getDataSource()).thenReturn(dataSource);
        when(col.isAscending()).thenReturn(ascending);
        return col;
    }

    /**
     * Unlike {@link #indexColumn}, mocks the {@link MimerTableIndexColumn} subclass so {@code
     * appendIndexColumnModifiers}' {@code instanceof} check picks up the algorithm/collation -
     * {@code getIndex()} must also be stubbed since the manager's clustered defense re-checks
     * {@code indexColumn.getIndex().getIndexType()} independently of the owning {@link #index}
     * field.
     */
    private DBSTableIndexColumn mimerIndexColumn(String name, boolean ascending, String algorithm) {
        return mimerIndexColumn(name, ascending, algorithm, null);
    }

    private DBSTableIndexColumn mimerIndexColumn(String name, boolean ascending, String algorithm, String collation) {
        MimerTableIndexColumn col = mock(MimerTableIndexColumn.class);
        when(col.getName()).thenReturn(name);
        when(col.getDataSource()).thenReturn(dataSource);
        when(col.isAscending()).thenReturn(ascending);
        when(col.getAlgorithm()).thenReturn(algorithm);
        when(col.getCollation()).thenReturn(collation);
        doReturn(index).when(col).getIndex();
        return col;
    }

    private GenericTableColumn tableColumn(String name) {
        GenericTableColumn col = mock(GenericTableColumn.class);
        when(col.getName()).thenReturn(name);
        when(col.getDataSource()).thenReturn(dataSource);
        return col;
    }
}
