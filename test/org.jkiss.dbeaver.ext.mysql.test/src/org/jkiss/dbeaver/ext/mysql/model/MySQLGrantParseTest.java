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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for {@link MySQLGrant#parseGrant} and its helpers: parsing of {@code SHOW GRANTS}
 * lines into grants, including table/column/routine/global grants and column-list quoting.
 */
public class MySQLGrantParseTest extends DBeaverUnitTest {

    private static final MySQLUser USER = new MySQLUser(null, null);

    // Resolves any requested privilege to an OBJECTS-kind privilege of that name (admin ones aside).
    private final Map<String, MySQLPrivilege> resolved = new HashMap<>();

    @Nullable
    private MySQLPrivilege resolve(@NotNull String name) {
        MySQLPrivilege.Kind kind = name.equalsIgnoreCase("SUPER") || name.equalsIgnoreCase("RELOAD")
            ? MySQLPrivilege.Kind.ADMIN : MySQLPrivilege.Kind.OBJECTS;
        return resolved.computeIfAbsent(name.toUpperCase(), n -> new MySQLPrivilege(null, name, "Tables", "", kind));
    }

    @NotNull
    private MySQLPrivilege privilege(@NotNull String name) {
        return resolved.get(name.toUpperCase());
    }

    @Nullable
    private MySQLGrant parse(@NotNull String grantString) throws DBException {
        return MySQLGrant.parseGrant(USER, grantString, this::resolve);
    }

    // ---- table grants ----

    @Test
    public void parsesTableGrantWithMultiplePrivileges() throws DBException {
        MySQLGrant grant = parse("GRANT SELECT, INSERT ON `mydb`.`t` TO 'u'@'%'");
        Assertions.assertNotNull(grant);
        Assertions.assertEquals(MySQLGrant.ObjectType.TABLE, grant.getObjectType());
        Assertions.assertEquals("mydb", grant.getCatalog());
        Assertions.assertEquals("t", grant.getTable());
        Assertions.assertFalse(grant.isGrantOption());
        Assertions.assertTrue(Arrays.asList(grant.getPrivileges()).contains(privilege("SELECT")));
        Assertions.assertTrue(Arrays.asList(grant.getPrivileges()).contains(privilege("INSERT")));
    }

    @Test
    public void parsesWithGrantOption() throws DBException {
        MySQLGrant grant = parse("GRANT SELECT ON `mydb`.`t` TO 'u'@'%' WITH GRANT OPTION");
        Assertions.assertNotNull(grant);
        Assertions.assertTrue(grant.isGrantOption());
    }

    @Test
    public void parsesAllPrivileges() throws DBException {
        MySQLGrant grant = parse("GRANT ALL PRIVILEGES ON `mydb`.* TO 'u'@'%'");
        Assertions.assertNotNull(grant);
        Assertions.assertTrue(grant.isAllPrivileges());
        Assertions.assertTrue(grant.isAllTables());
    }

    // ---- global grants ----

    @Test
    public void parsesGlobalGrant() throws DBException {
        MySQLGrant grant = parse("GRANT SELECT ON *.* TO 'u'@'%'");
        Assertions.assertNotNull(grant);
        Assertions.assertTrue(grant.isAllCatalogs());
        Assertions.assertTrue(grant.isAllTables());
    }

    // ---- routine grants ----

    @Test
    public void parsesProcedureGrant() throws DBException {
        MySQLGrant grant = parse("GRANT EXECUTE ON PROCEDURE `mydb`.`p` TO 'u'@'%'");
        Assertions.assertNotNull(grant);
        Assertions.assertEquals(MySQLGrant.ObjectType.PROCEDURE, grant.getObjectType());
        Assertions.assertEquals("p", grant.getTable());
    }

    @Test
    public void parsesFunctionGrant() throws DBException {
        MySQLGrant grant = parse("GRANT EXECUTE ON FUNCTION `mydb`.`f` TO 'u'@'%'");
        Assertions.assertNotNull(grant);
        Assertions.assertEquals(MySQLGrant.ObjectType.FUNCTION, grant.getObjectType());
    }

    // ---- column grants ----

    @Test
    public void parsesColumnGrantAndKeepsPrivilegeVisible() throws DBException {
        MySQLGrant grant = parse("GRANT SELECT (`col1`, `col2`), UPDATE (`col3`) ON `mydb`.`t` TO 'u'@'%'");
        Assertions.assertNotNull(grant);
        // A column-only grant must not be treated as empty (would otherwise be dropped in the editor)
        Assertions.assertTrue(grant.hasNonAdminPrivileges());
        Assertions.assertTrue(grant.hasColumnPrivilege(privilege("SELECT"), "col1"));
        Assertions.assertTrue(grant.hasColumnPrivilege(privilege("SELECT"), "col2"));
        Assertions.assertTrue(grant.hasColumnPrivilege(privilege("UPDATE"), "col3"));
    }

    @Test
    public void columnNameWithCommaInsideBackticksIsOneColumn() throws DBException {
        MySQLGrant grant = parse("GRANT SELECT (`a,b`) ON `mydb`.`t` TO 'u'@'%'");
        Assertions.assertNotNull(grant);
        Set<String> columns = grant.getColumnPrivileges().get(privilege("SELECT"));
        Assertions.assertEquals(Set.of("a,b"), columns);
    }

    @Test
    public void escapedBacktickInColumnNameIsPreserved() throws DBException {
        MySQLGrant grant = parse("GRANT SELECT (`a``b`) ON `mydb`.`t` TO 'u'@'%'");
        Assertions.assertNotNull(grant);
        Set<String> columns = grant.getColumnPrivileges().get(privilege("SELECT"));
        Assertions.assertEquals(Set.of("a`b"), columns);
    }

    @Test
    public void unquotedColumnListIsParsed() throws DBException {
        MySQLGrant grant = parse("GRANT SELECT (col1, col2) ON `mydb`.`t` TO 'u'@'%'");
        Assertions.assertNotNull(grant);
        Assertions.assertTrue(grant.hasColumnPrivilege(privilege("SELECT"), "col1"));
        Assertions.assertTrue(grant.hasColumnPrivilege(privilege("SELECT"), "col2"));
    }

    // ---- unparseable input ----

    @Test
    public void returnsNullForUnrecognizedLine() throws DBException {
        Assertions.assertNull(parse("REVOKE SELECT ON `mydb`.`t` FROM 'u'@'%'"));
    }

    // ---- helper parsers ----

    @Test
    public void splitPrivilegesKeepsColumnListsIntact() {
        List<String> tokens = MySQLGrant.splitPrivileges("SELECT (col1, col2), INSERT, UPDATE (col3)");
        Assertions.assertEquals(List.of("SELECT (col1, col2)", " INSERT", " UPDATE (col3)"), tokens);
    }

    @Test
    public void parseColumnListHandlesQuotingAndEscaping() {
        Assertions.assertEquals(List.of("col1", "col2"), MySQLGrant.parseColumnList("`col1`, `col2`"));
        Assertions.assertEquals(List.of("a,b"), MySQLGrant.parseColumnList("`a,b`"));
        Assertions.assertEquals(List.of("a`b"), MySQLGrant.parseColumnList("`a``b`"));
        Assertions.assertEquals(List.of("plain"), MySQLGrant.parseColumnList("  plain  "));
    }
}
