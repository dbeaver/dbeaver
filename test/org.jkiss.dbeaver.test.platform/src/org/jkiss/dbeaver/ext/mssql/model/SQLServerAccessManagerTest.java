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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAccessState;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.util.List;

public class SQLServerAccessManagerTest extends DBeaverUnitTest {
    private final SQLServerDataSource dataSource = Mockito.mock(SQLServerDataSource.class);
    private final SQLServerLogin login = new SQLServerLogin(dataSource, "DOMAIN\\account]name");
    private final SQLServerAccessManager manager = new SQLServerAccessManager(login);
    private final DBAAccessState.Scope server = new DBAAccessState.Scope("", "Server");
    private final DBAAccessState.Scope database = new DBAAccessState.Scope("db]name", "Database");
    private final DBAAccessState.Target target = new DBAAccessState.Target(
        "OBJECT::[schema].[table] ([column])", "Column", List.of("SELECT", "UPDATE"), true);

    public SQLServerAccessManagerTest() {
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new SQLServerDialectMssql());
    }

    @Test
    public void grantsServerRoleToWindowsPrincipal() throws DBException {
        DBAAccessState before = serverState(false, List.of());
        DBAAccessState after = serverState(true, List.of());
        Assertions.assertEquals("ALTER SERVER ROLE [securityadmin] ADD MEMBER [DOMAIN\\account]]name];\n",
            manager.generateChanges(before, after));
    }

    @Test
    public void removesServerRoleMembership() throws DBException {
        Assertions.assertEquals("ALTER SERVER ROLE [securityadmin] DROP MEMBER [DOMAIN\\account]]name];\n",
            manager.generateChanges(serverState(true, List.of()), serverState(false, List.of())));
    }

    @Test
    public void createsDatabaseUserBeforeGrantingRole() throws DBException {
        DBAAccessState before = databaseState(false, "mapped user", "dbo", false);
        DBAAccessState after = databaseState(true, "mapped user", "schema]name", true);
        Assertions.assertEquals("CREATE USER [mapped user] FOR LOGIN [DOMAIN\\account]]name] WITH DEFAULT_SCHEMA = [schema]]name];\n"
            + "ALTER ROLE [db_datareader] ADD MEMBER [mapped user];\n", manager.generateChanges(before, after));
    }

    @Test
    public void dropsOnlyMappedUserWhenUnmapping() throws DBException {
        Assertions.assertEquals("DROP USER [mapped user];\n", manager.generateChanges(
            databaseState(true, "mapped user", "dbo", true), databaseState(false, "mapped user", "dbo", true)));
    }

    @Test
    public void renamesUserAndChangesDefaultSchema() throws DBException {
        Assertions.assertEquals("ALTER USER [old name] WITH NAME = [new name];\n"
            + "ALTER USER [new name] WITH DEFAULT_SCHEMA = [new schema];\n", manager.generateChanges(
                databaseState(true, "old name", "dbo", false), databaseState(true, "new name", "new schema", false)));
    }

    @Test
    public void preventsChangesToImplicitPublicMembership() {
        DBAAccessState before = new DBAAccessState(server, null,
            List.of(new DBAAccessState.Role("public", true, false)), List.of(target), List.of());
        DBAAccessState after = new DBAAccessState(server, null,
            List.of(new DBAAccessState.Role("public", false, false)), List.of(target), List.of());
        Assertions.assertThrows(DBException.class, () -> manager.generateChanges(before, after));
    }

    @Test
    public void preventsDroppingProtectedMapping() {
        DBAAccessState before = new DBAAccessState(database, new DBAAccessState.Mapping(true, "dbo", "dbo", false, false),
            List.of(), List.of(target), List.of());
        DBAAccessState after = new DBAAccessState(database, new DBAAccessState.Mapping(false, "dbo", "dbo", true, true),
            List.of(), List.of(target), List.of());
        Assertions.assertThrows(DBException.class, () -> manager.generateChanges(before, after));
    }

    @Test
    public void omitsUnsupportedDefaultSchema() throws DBException {
        DBAAccessState before = new DBAAccessState(database, new DBAAccessState.Mapping(false, "group", "", true, false),
            List.of(), List.of(target), List.of());
        DBAAccessState after = new DBAAccessState(database, new DBAAccessState.Mapping(true, "group", "", true, false),
            List.of(), List.of(target), List.of());
        Assertions.assertEquals("CREATE USER [group] FOR LOGIN [DOMAIN\\account]]name];\n", manager.generateChanges(before, after));
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    public void loadsWindowsGroupSchemaCapability(boolean mapped, boolean supported) throws Exception {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        DBPDriver driver = Mockito.mock(DBPDriver.class);
        JDBCRemoteInstance instance = Mockito.mock(JDBCRemoteInstance.class);
        JDBCExecutionContext context = Mockito.mock(JDBCExecutionContext.class);
        JDBCSession session = Mockito.mock(JDBCSession.class);
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(container.getDriver()).thenReturn(driver);
        Mockito.when(driver.getId()).thenReturn("microsoft");
        Mockito.when(dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2012_VERSION_MAJOR, 0)).thenReturn(supported);
        Mockito.when(dataSource.hasDefaultInstance()).thenReturn(true);
        Mockito.when(dataSource.getDefaultInstance()).thenReturn(instance);
        Mockito.when(instance.getDataSource()).thenReturn(dataSource);
        Mockito.when(instance.getDefaultContext(Mockito.any(), Mockito.anyBoolean())).thenReturn(context);
        Mockito.when(context.isConnected()).thenReturn(true);
        Mockito.when(context.openSession(Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(session);
        Mockito.when(session.getProgressMonitor()).thenReturn(monitor);
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
            JDBCResultSet result = Mockito.mock(JDBCResultSet.class);
            Mockito.when(statement.executeQuery()).thenReturn(result);
            if (sql.contains("database_principals WHERE sid") && mapped || sql.startsWith("SELECT type FROM sys.server_principals")) {
                Mockito.when(result.next()).thenReturn(true, false);
                Mockito.when(result.getString("type")).thenReturn("G");
                Mockito.when(result.getString("name")).thenReturn("group");
                Mockito.when(result.getInt("principal_id")).thenReturn(5);
            }
            return statement;
        });
        DBAAccessState state = manager.load(monitor, database);
        Assertions.assertEquals(mapped, state.mapping().present());
        Assertions.assertEquals(supported, state.mapping().schemaEditable());
        Assertions.assertEquals(supported, state.mapping().schemaNullable());
    }

    @Test
    public void clearsWindowsGroupDefaultSchema() throws DBException {
        DBAAccessState before = new DBAAccessState(database, new DBAAccessState.Mapping(true, "group", "shared", true, true, true),
            List.of(), List.of(target), List.of());
        DBAAccessState after = new DBAAccessState(database, new DBAAccessState.Mapping(true, "group", "", true, true, true),
            List.of(), List.of(target), List.of());
        Assertions.assertEquals("ALTER USER [group] WITH DEFAULT_SCHEMA = NULL;\n", manager.generateChanges(before, after));
    }

    @Test
    public void rejectsClearingDefaultSchemaWhenUnsupported() {
        Assertions.assertThrows(DBException.class, () -> manager.generateChanges(
            databaseState(true, "user", "dbo", false), databaseState(true, "user", "", false)));
    }

    @Test
    public void createsWindowsGroupWithDefaultSchema() throws DBException {
        DBAAccessState before = databaseState(false, "group", "", false);
        DBAAccessState after = databaseState(true, "group", "shared", false);
        Assertions.assertEquals("CREATE USER [group] FOR LOGIN [DOMAIN\\account]]name] WITH DEFAULT_SCHEMA = [shared];\n",
            manager.generateChanges(before, after));
    }

    @ParameterizedTest
    @CsvSource({
        "U,EXECUTE,false", "U,RECEIVE,false", "U,SELECT,true", "U,UNMASK,true", "U,VIEW CHANGE TRACKING,true",
        "V,EXECUTE,false", "V,UPDATE,true", "V,UNMASK,false", "P,EXECUTE,true", "P,INSERT,false", "P,REFERENCES,true",
        "FN,EXECUTE,true", "FN,SELECT,false", "IF,UPDATE,true", "TF,UPDATE,false", "TF,SELECT,true",
        "SQ,RECEIVE,true", "SQ,INSERT,false", "SO,UPDATE,true", "SO,SELECT,false", "SN,EXECUTE,true", "SN,REFERENCES,false"
    })
    public void filtersPermissionsByObjectType(@NotNull String type, @NotNull String permission, boolean supported) {
        Assertions.assertEquals(supported,
            SQLServerAccessManager.getObjectPermissions(type, List.of(permission), false).contains(permission));
    }

    @ParameterizedTest
    @CsvSource({
        "U,UNMASK,true", "V,UNMASK,false", "V,SELECT,true", "IF,UPDATE,true", "TF,UPDATE,false", "FT,REFERENCES,true",
        "P,SELECT,false", "FN,SELECT,false", "SQ,SELECT,false", "U,CONTROL,false"
    })
    public void filtersPermissionsByColumnType(@NotNull String type, @NotNull String permission, boolean supported) {
        Assertions.assertEquals(supported,
            SQLServerAccessManager.getObjectPermissions(type, List.of(permission), true).contains(permission));
    }

    @Test
    public void keepsObjectPermissionsWithinServerCapabilities() {
        Assertions.assertEquals(List.of("SELECT", "CONTROL"),
            SQLServerAccessManager.getObjectPermissions("U", List.of("SELECT", "CONTROL", "EXECUTE"), false));
        Assertions.assertTrue(SQLServerAccessManager.getObjectPermissions("unknown", List.of("SELECT", "CONTROL"), false).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(DBAAccessState.PermissionState.class)
    public void generatesExplicitColumnPermission(@NotNull DBAAccessState.PermissionState state) throws DBException {
        DBAAccessState before = serverState(false, List.of());
        DBAAccessState after = serverState(false, List.of(permission(state)));
        String operation = state == DBAAccessState.PermissionState.DENY ? "DENY"
            : state == DBAAccessState.PermissionState.REVOKE ? "REVOKE" : "GRANT";
        String expected = operation + " SELECT ON OBJECT::[schema].[table] ([column])"
            + (state == DBAAccessState.PermissionState.REVOKE ? " FROM " : " TO ") + "[DOMAIN\\account]]name]"
            + (state == DBAAccessState.PermissionState.GRANT_WITH_OPTION ? " WITH GRANT OPTION" : "") + " AS [grantor]]name];\n";
        Assertions.assertEquals(expected, manager.generateChanges(before, after));
    }

    @Test
    public void removesGrantOptionWithoutRevokingPermission() throws DBException {
        Assertions.assertEquals("REVOKE GRANT OPTION FOR SELECT ON OBJECT::[schema].[table] ([column])"
            + " FROM [DOMAIN\\account]]name] CASCADE AS [grantor]]name];\n", manager.generateChanges(
                serverState(false, List.of(permission(DBAAccessState.PermissionState.GRANT_WITH_OPTION))),
                serverState(false, List.of(new DBAAccessState.Permission(target, "SELECT", DBAAccessState.PermissionState.GRANT,
                    "grantor]name", true, true)))));
    }

    @Test
    public void revokesOldDenyBeforeGrant() throws DBException {
        String sql = manager.generateChanges(serverState(false, List.of(permission(DBAAccessState.PermissionState.DENY))),
            serverState(false, List.of(permission(DBAAccessState.PermissionState.GRANT))));
        Assertions.assertTrue(sql.startsWith("REVOKE SELECT"));
        Assertions.assertTrue(sql.contains(";\nGRANT SELECT"));
        Assertions.assertFalse(sql.contains("CASCADE"));
    }

    @ParameterizedTest
    @EnumSource(value = DBAAccessState.PermissionState.class, names = {"GRANT_WITH_OPTION", "DENY", "REVOKE"})
    public void restoresColumnDenyAfterTablePermissionChange(@NotNull DBAAccessState.PermissionState state) throws DBException {
        DBAAccessState.Target table = tableTarget();
        DBAAccessState.Permission column = permission(DBAAccessState.PermissionState.DENY);
        DBAAccessState before = permissionState(List.of(new DBAAccessState.Permission(table, "SELECT",
            DBAAccessState.PermissionState.GRANT, "dbo", true), column));
        DBAAccessState after = permissionState(List.of(new DBAAccessState.Permission(table, "SELECT", state, "dbo", true), column));
        String sql = manager.generateChanges(before, after);
        Assertions.assertTrue(sql.endsWith("DENY SELECT ON OBJECT::[schema].[table] ([column])"
            + " TO [DOMAIN\\account]]name] CASCADE AS [grantor]]name];\n"));
    }

    @Test
    public void restoresColumnGrantOptionWithoutRequiringCascade() throws DBException {
        DBAAccessState.Permission column = permission(DBAAccessState.PermissionState.GRANT_WITH_OPTION);
        DBAAccessState.Permission table = new DBAAccessState.Permission(tableTarget(), "SELECT",
            DBAAccessState.PermissionState.GRANT_WITH_OPTION, "dbo", true);
        String sql = manager.generateChanges(permissionState(List.of(column)), permissionState(List.of(column, table)));
        Assertions.assertTrue(sql.endsWith("GRANT SELECT ON OBJECT::[schema].[table] ([column])"
            + " TO [DOMAIN\\account]]name] WITH GRANT OPTION AS [grantor]]name];\n"));
    }

    @Test
    public void upgradesGrantWithoutRevokingColumnGrantOptions() throws DBException {
        DBAAccessState.Target table = tableTarget();
        DBAAccessState.Permission column = permission(DBAAccessState.PermissionState.GRANT_WITH_OPTION);
        DBAAccessState before = permissionState(List.of(new DBAAccessState.Permission(table, "SELECT",
            DBAAccessState.PermissionState.GRANT, "dbo", true), column));
        DBAAccessState after = permissionState(List.of(new DBAAccessState.Permission(table, "SELECT",
            DBAAccessState.PermissionState.GRANT_WITH_OPTION, "dbo", true), column));
        String sql = manager.generateChanges(before, after);
        Assertions.assertTrue(sql.startsWith("GRANT SELECT ON OBJECT::[schema].[table] TO"));
        Assertions.assertFalse(sql.contains("REVOKE"));
    }

    @Test
    public void appliesEditedColumnAfterTableRegardlessOfListOrder() throws DBException {
        DBAAccessState.Permission table = new DBAAccessState.Permission(tableTarget(), "SELECT",
            DBAAccessState.PermissionState.GRANT, "dbo", true);
        String sql = manager.generateChanges(permissionState(List.of()),
            permissionState(List.of(permission(DBAAccessState.PermissionState.DENY), table)));
        Assertions.assertTrue(sql.startsWith("GRANT SELECT ON OBJECT::[schema].[table] TO"));
        Assertions.assertTrue(sql.contains(";\nDENY SELECT ON OBJECT::[schema].[table] ([column])"));
    }

    @Test
    public void preservesColumnDenyWhenTablePermissionIsRemoved() throws DBException {
        DBAAccessState.Permission column = permission(DBAAccessState.PermissionState.DENY);
        DBAAccessState.Permission table = new DBAAccessState.Permission(tableTarget(), "SELECT",
            DBAAccessState.PermissionState.GRANT, "dbo", true);
        String sql = manager.generateChanges(permissionState(List.of(column, table)), permissionState(List.of(column)));
        Assertions.assertTrue(sql.startsWith("REVOKE SELECT ON OBJECT::[schema].[table] FROM"));
        Assertions.assertTrue(sql.contains(";\nDENY SELECT ON OBJECT::[schema].[table] ([column])"));
    }

    @NotNull
    private DBAAccessState.Target tableTarget() {
        return new DBAAccessState.Target("OBJECT::[schema].[table]", "Table", List.of("SELECT", "UPDATE"), true);
    }

    @NotNull
    private DBAAccessState permissionState(@NotNull List<DBAAccessState.Permission> permissions) {
        return new DBAAccessState(server, null, List.of(), List.of(target, tableTarget()), permissions);
    }

    @Test
    public void rejectsPermissionOutsideCatalog() {
        DBAAccessState.Permission invalid = new DBAAccessState.Permission(target, "SELECT; DROP DATABASE test",
            DBAAccessState.PermissionState.GRANT, null, true);
        Assertions.assertThrows(DBException.class, () -> manager.generateChanges(serverState(false, List.of()),
            serverState(false, List.of(invalid))));
    }

    @Test
    public void preservesUnsupportedReadOnlyPermissions() throws DBException {
        DBAAccessState.Target unsupported = new DBAAccessState.Target("unsupported", "unsupported", List.of(), false);
        DBAAccessState.Permission permission = new DBAAccessState.Permission(unsupported, "CONTROL",
            DBAAccessState.PermissionState.GRANT, "dbo", false);
        DBAAccessState state = new DBAAccessState(server, null, List.of(), List.of(unsupported), List.of(permission));
        Assertions.assertEquals("", manager.generateChanges(state, state));
        DBAAccessState changed = new DBAAccessState(server, null, List.of(), List.of(unsupported), List.of());
        Assertions.assertThrows(DBException.class, () -> manager.generateChanges(state, changed));
    }

    @Test
    public void rejectsCrossScopeChanges() {
        Assertions.assertThrows(DBException.class, () -> manager.generateChanges(
            serverState(false, List.of()), databaseState(true, "user", "dbo", false)));
    }

    @Test
    public void leavesUnchangedStateEmpty() throws DBException {
        DBAAccessState state = databaseState(true, "user", "dbo", true);
        Assertions.assertEquals("", manager.generateChanges(state, state));
    }

    @ParameterizedTest
    @EnumSource(value = DBAAccessState.PermissionState.class, names = {"GRANT", "DENY", "REVOKE"})
    public void requiresExplicitCascadeWhenRemovingGrantOption(@NotNull DBAAccessState.PermissionState state) {
        Assertions.assertThrows(DBException.class, () -> manager.generateChanges(
            serverState(false, List.of(permission(DBAAccessState.PermissionState.GRANT_WITH_OPTION))),
            serverState(false, List.of(permission(state)))));
    }

    @Test
    public void rejectsDuplicatePermissionEntries() {
        DBAAccessState.Permission permission = permission(DBAAccessState.PermissionState.GRANT);
        Assertions.assertThrows(DBException.class, () -> manager.generateChanges(serverState(false, List.of()),
            serverState(false, List.of(permission, permission))));
    }

    @NotNull
    private DBAAccessState.Permission permission(@NotNull DBAAccessState.PermissionState state) {
        return new DBAAccessState.Permission(target, "SELECT", state, "grantor]name", true);
    }

    @NotNull
    private DBAAccessState serverState(boolean granted, @NotNull List<DBAAccessState.Permission> permissions) {
        return new DBAAccessState(server, null, List.of(new DBAAccessState.Role("securityadmin", granted, true)),
            List.of(target), permissions);
    }

    @NotNull
    private DBAAccessState databaseState(boolean mapped, @NotNull String name, @NotNull String schema, boolean granted) {
        return new DBAAccessState(database, new DBAAccessState.Mapping(mapped, name, schema, true, true),
            List.of(new DBAAccessState.Role("db_datareader", granted, true)), List.of(target), List.of());
    }
}
