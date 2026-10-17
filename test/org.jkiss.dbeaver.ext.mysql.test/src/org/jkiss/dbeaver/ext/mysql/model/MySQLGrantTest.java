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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link MySQLGrant}: object matching, column privileges, emptiness/relevance checks.
 */
public class MySQLGrantTest extends DBeaverUnitTest {

    private static final MySQLUser USER = new MySQLUser(null, null);

    @NotNull
    private static MySQLPrivilege priv(@NotNull String name, @NotNull MySQLPrivilege.Kind kind) {
        return new MySQLPrivilege(null, name, "Tables", "", kind);
    }

    @NotNull
    private static MySQLPrivilege object(@NotNull String name) {
        return priv(name, MySQLPrivilege.Kind.OBJECTS);
    }

    @NotNull
    private static MySQLGrant tableGrant(
        @NotNull String catalog,
        @NotNull String table,
        @NotNull List<MySQLPrivilege> privileges,
        boolean grantOption
    ) {
        return new MySQLGrant(
            USER, new ArrayList<>(privileges), catalog, table, false, grantOption, MySQLGrant.ObjectType.TABLE);
    }

    @NotNull
    private static MySQLGrant procedureGrant(@NotNull String catalog, @NotNull String name, @NotNull MySQLGrant.ObjectType type) {
        return new MySQLGrant(USER, List.of(object("EXECUTE")), catalog, name, false, false, type);
    }

    @NotNull
    private static MySQLCatalog catalog(@NotNull String name) {
        MySQLCatalog catalog = Mockito.mock(MySQLCatalog.class);
        Mockito.when(catalog.getName()).thenReturn(name);
        return catalog;
    }

    @NotNull
    private static MySQLTableBase table(@NotNull String name) {
        MySQLTableBase table = Mockito.mock(MySQLTableBase.class);
        Mockito.when(table.getName()).thenReturn(name);
        return table;
    }

    @NotNull
    private static MySQLProcedure procedure(@NotNull String name, @NotNull DBSProcedureType type) {
        MySQLProcedure procedure = Mockito.mock(MySQLProcedure.class);
        Mockito.when(procedure.getName()).thenReturn(name);
        Mockito.when(procedure.getProcedureType()).thenReturn(type);
        return procedure;
    }

    // ---- catalog matching ----

    @Test
    public void matchesCatalogByName() {
        MySQLGrant grant = tableGrant("mydb", "t", List.of(object("SELECT")), false);
        Assertions.assertTrue(grant.matches(catalog("mydb")));
        Assertions.assertFalse(grant.matches(catalog("otherdb")));
    }

    @Test
    public void matchesCatalogCaseInsensitively() {
        // Catalog names are matched case-insensitively (SQLUtils.matchesLike), consistent with table matching
        MySQLGrant grant = tableGrant("mydb", "t", List.of(object("SELECT")), false);
        Assertions.assertTrue(grant.matches(catalog("MYDB")));
    }

    @Test
    public void globalGrantMatchesNullCatalogOnly() {
        MySQLGrant global = tableGrant("*", "*", List.of(object("SELECT")), false);
        Assertions.assertTrue(global.isAllCatalogs());
        Assertions.assertTrue(global.matches((MySQLCatalog) null));
        // A global grant deliberately does not match a concrete catalog
        Assertions.assertFalse(global.matches(catalog("mydb")));
    }

    @Test
    public void nonGlobalGrantDoesNotMatchNullCatalog() {
        MySQLGrant grant = tableGrant("mydb", "t", List.of(object("SELECT")), false);
        Assertions.assertFalse(grant.matches((MySQLCatalog) null));
    }

    // ---- table matching ----

    @Test
    public void matchesTableCaseInsensitively() {
        MySQLGrant grant = tableGrant("mydb", "MyTable", List.of(object("SELECT")), false);
        Assertions.assertTrue(grant.matches(table("mytable")));
        Assertions.assertFalse(grant.matches(table("other")));
    }

    @Test
    public void allTablesGrantMatchesNullTable() {
        MySQLGrant grant = tableGrant("mydb", "*", List.of(object("SELECT")), false);
        Assertions.assertTrue(grant.isAllTables());
        Assertions.assertTrue(grant.matches((MySQLTableBase) null));
    }

    @Test
    public void procedureGrantDoesNotMatchTable() {
        MySQLGrant grant = procedureGrant("mydb", "myproc", MySQLGrant.ObjectType.PROCEDURE);
        Assertions.assertFalse(grant.matches(table("myproc")));
    }

    // ---- procedure matching ----

    @Test
    public void matchesProcedureByTypeAndName() {
        MySQLGrant grant = procedureGrant("mydb", "myproc", MySQLGrant.ObjectType.PROCEDURE);
        Assertions.assertTrue(grant.matchesProcedure(procedure("MyProc", DBSProcedureType.PROCEDURE)));
        Assertions.assertFalse(grant.matchesProcedure(procedure("other", DBSProcedureType.PROCEDURE)));
    }

    @Test
    public void functionGrantDoesNotMatchProcedureOfSameName() {
        MySQLGrant grant = procedureGrant("mydb", "f", MySQLGrant.ObjectType.FUNCTION);
        Assertions.assertTrue(grant.matchesProcedure(procedure("f", DBSProcedureType.FUNCTION)));
        Assertions.assertFalse(grant.matchesProcedure(procedure("f", DBSProcedureType.PROCEDURE)));
    }

    @Test
    public void tableGrantNeverMatchesProcedure() {
        MySQLGrant grant = tableGrant("mydb", "t", List.of(object("SELECT")), false);
        Assertions.assertFalse(grant.matchesProcedure(procedure("t", DBSProcedureType.PROCEDURE)));
        Assertions.assertFalse(grant.matchesProcedure(null));
    }

    // ---- isStatic / isAllCatalogs / isAllTables ----

    @Test
    public void isStaticForGlobalOrEmptyCatalog() {
        Assertions.assertTrue(tableGrant("*", "*", List.of(object("SELECT")), false).isStatic());
        Assertions.assertFalse(tableGrant("mydb", "t", List.of(object("SELECT")), false).isStatic());
    }

    // ---- emptiness ----

    @Test
    public void isEmptyOnlyWhenNothingGranted() {
        Assertions.assertTrue(tableGrant("mydb", "t", List.of(), false).isEmpty());
        Assertions.assertFalse(tableGrant("mydb", "t", List.of(object("SELECT")), false).isEmpty());
        Assertions.assertFalse(tableGrant("mydb", "t", List.of(), true).isEmpty());
    }

    @Test
    public void grantWithOnlyColumnPrivilegesIsNotEmpty() {
        MySQLGrant grant = tableGrant("mydb", "t", List.of(), false);
        grant.addColumnPrivilege(object("SELECT"), "col1");
        Assertions.assertFalse(grant.isEmpty());
    }

    // ---- hasNonAdminPrivileges (regression: column-only grants must count) ----

    @Test
    public void hasNonAdminPrivilegesForTablePrivilege() {
        Assertions.assertTrue(tableGrant("mydb", "t", List.of(object("SELECT")), false).hasNonAdminPrivileges());
    }

    @Test
    public void hasNoNonAdminPrivilegesForAdminOnly() {
        MySQLGrant grant = tableGrant("mydb", "t", List.of(priv("SUPER", MySQLPrivilege.Kind.ADMIN)), false);
        Assertions.assertFalse(grant.hasNonAdminPrivileges());
    }

    @Test
    public void hasNonAdminPrivilegesForColumnOnlyGrant() {
        // Regression: a column-only grant keeps its privilege in the column map. If this is not
        // counted, processGrants() treats the grant as empty and drops the column grant entirely.
        MySQLGrant grant = tableGrant("mydb", "t", List.of(), false);
        grant.addColumnPrivilege(object("SELECT"), "col1");
        Assertions.assertTrue(grant.hasNonAdminPrivileges());
    }

    // ---- column privileges: case-insensitive, original case preserved ----

    @Test
    public void columnPrivilegePreservesOriginalCaseButMatchesCaseInsensitively() {
        MySQLPrivilege select = object("SELECT");
        MySQLGrant grant = tableGrant("mydb", "t", List.of(), false);
        grant.addColumnPrivilege(select, "MyColumn");
        Assertions.assertTrue(grant.hasColumnPrivilege(select, "mycolumn"));
        Assertions.assertTrue(grant.hasColumnPrivileges("MYCOLUMN"));
        Assertions.assertEquals("MyColumn", grant.getColumnPrivileges().get(select).iterator().next());
    }

    @Test
    public void addColumnPrivilegeIsIdempotentIgnoringCase() {
        MySQLPrivilege select = object("SELECT");
        MySQLGrant grant = tableGrant("mydb", "t", List.of(), false);
        grant.addColumnPrivilege(select, "col");
        grant.addColumnPrivilege(select, "COL");
        Assertions.assertEquals(1, grant.getColumnPrivileges().get(select).size());
    }

    @Test
    public void removeColumnPrivilegeDropsPrivilegeWhenLastColumnGone() {
        MySQLPrivilege select = object("SELECT");
        MySQLGrant grant = tableGrant("mydb", "t", List.of(), false);
        grant.addColumnPrivilege(select, "col");
        grant.removeColumnPrivilege(select, "COL");
        Assertions.assertFalse(grant.getColumnPrivileges().containsKey(select));
        Assertions.assertFalse(grant.hasColumnPrivilege(select, "col"));
    }

    @Test
    public void objectSpecUsesStoredWildcardSchema() {
        // A `tenant\_%`.* grant shows up under tenant_1, but must be revoked by its stored pattern
        MySQLGrant grant = tableGrant("tenant\\_%", "*", List.of(object("SELECT")), false);
        Assertions.assertTrue(grant.matches(catalog("tenant_1")));
        Assertions.assertEquals("`tenant\\_%`.*", grant.getObjectSpec());
    }

    @Test
    public void objectSpecForTableAndRoutineGrants() {
        Assertions.assertEquals("`mydb`.`t`", tableGrant("mydb", "t", List.of(), false).getObjectSpec());
        Assertions.assertEquals(
            "PROCEDURE `mydb`.`p`", procedureGrant("mydb", "p", MySQLGrant.ObjectType.PROCEDURE).getObjectSpec());
        Assertions.assertEquals(
            "FUNCTION `mydb`.`f`", procedureGrant("mydb", "f", MySQLGrant.ObjectType.FUNCTION).getObjectSpec());
    }

    @Test
    public void objectSpecEscapesBackticks() {
        Assertions.assertEquals("`my``db`.`t``1`", tableGrant("my`db", "t`1", List.of(), false).getObjectSpec());
    }

    @NotNull
    private static MySQLUser userWithLowerCaseTableNames(int lowerCaseTableNames) {
        MySQLDialect dialect = Mockito.mock(MySQLDialect.class);
        Mockito.when(dialect.useCaseInsensitiveNameLookup()).thenReturn(lowerCaseTableNames != 0);
        MySQLDataSource dataSource = Mockito.mock(MySQLDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        return new MySQLUser(dataSource, null);
    }

    @Test
    public void tableAndSchemaMatchingIsCaseSensitiveWhenServerIs() {
        // lower_case_table_names=0: Foo and foo are different tables (and schemas) with distinct grants
        MySQLUser user = userWithLowerCaseTableNames(0);
        MySQLGrant grant = new MySQLGrant(user, new ArrayList<>(), "MyDb", "Foo", false, false, MySQLGrant.ObjectType.TABLE);
        Assertions.assertTrue(grant.matches(table("Foo")));
        Assertions.assertFalse(grant.matches(table("foo")));
        Assertions.assertTrue(grant.matches(catalog("MyDb")));
        Assertions.assertFalse(grant.matches(catalog("mydb")));
    }

    @Test
    public void tableAndSchemaMatchingIgnoresCaseWhenServerDoes() {
        MySQLUser user = userWithLowerCaseTableNames(1);
        MySQLGrant grant = new MySQLGrant(user, new ArrayList<>(), "MyDb", "Foo", false, false, MySQLGrant.ObjectType.TABLE);
        Assertions.assertTrue(grant.matches(table("foo")));
        Assertions.assertTrue(grant.matches(catalog("mydb")));
    }

    @Test
    public void caseSensitiveSchemaMatchingKeepsWildcards() {
        MySQLGrant grant = new MySQLGrant(
            userWithLowerCaseTableNames(0), new ArrayList<>(), "Tenant\\_%", "*", false, false, MySQLGrant.ObjectType.TABLE);
        Assertions.assertTrue(grant.matches(catalog("Tenant_1")));
        Assertions.assertFalse(grant.matches(catalog("tenant_1")));
        Assertions.assertFalse(grant.matches(catalog("TenantX1")));
    }

    @Test
    public void expandAllPrivilegesAllowsRevokingOnePrivilege() {
        MySQLPrivilege select = object("SELECT");
        MySQLPrivilege insert = object("INSERT");
        MySQLPrivilege grantOption = new MySQLPrivilege(null, MySQLPrivilege.GRANT_PRIVILEGE, "Tables", "", MySQLPrivilege.Kind.OBJECTS);
        MySQLGrant grant = new MySQLGrant(USER, new ArrayList<>(), "mydb", "*", true, false, MySQLGrant.ObjectType.TABLE);
        grant.expandAllPrivileges(List.of(select, insert, grantOption));
        grant.removePrivilege(select);
        Assertions.assertFalse(grant.isAllPrivileges());
        Assertions.assertEquals(List.of(insert), List.of(grant.getPrivileges()));
    }

    @Test
    public void expandAllPrivilegesKeepsExplicitGrantUntouched() {
        MySQLPrivilege select = object("SELECT");
        MySQLGrant grant = tableGrant("mydb", "t", List.of(select), false);
        grant.expandAllPrivileges(List.of(select, object("INSERT")));
        Assertions.assertEquals(List.of(select), List.of(grant.getPrivileges()));
    }

    @Test
    public void wildcardSchemaPatternForPatternGrantShownUnderConcreteSchema() {
        MySQLGrant grant = tableGrant("tenant\\_%", "*", List.of(object("SELECT")), false);
        Assertions.assertEquals("tenant\\_%", grant.getWildcardSchemaPattern(catalog("tenant_1")));
    }

    @Test
    public void noWildcardSchemaPatternForOrdinaryGrants() {
        Assertions.assertNull(tableGrant("mydb", "*", List.of(), false).getWildcardSchemaPattern(catalog("mydb")));
        // Table-level and routine grants can't use schema wildcards
        Assertions.assertNull(tableGrant("mydb", "t", List.of(), false).getWildcardSchemaPattern(catalog("mydb")));
        Assertions.assertNull(
            procedureGrant("mydb", "p", MySQLGrant.ObjectType.PROCEDURE).getWildcardSchemaPattern(catalog("mydb")));
        Assertions.assertNull(tableGrant("*", "*", List.of(), false).getWildcardSchemaPattern(catalog("mydb")));
    }
}
