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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDataSource;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDatabase;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDialectMssql;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerLogin;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SQLServerLoginManagerTest extends DBeaverUnitTest {

    private final SQLServerDataSource dataSource = Mockito.mock(SQLServerDataSource.class);
    private final SQLServerLoginManager manager = new SQLServerLoginManager();
    private final DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);

    @Test
    public void createsSqlLoginWithQuotedPassword() throws DBException {
        SQLServerLogin login = new SQLServerLogin(dataSource, "sql login");
        login.setPassword("Pass'word");

        Assertions.assertEquals(SQLServerLogin.LoginType.S, login.getLoginType());
        Assertions.assertEquals("CREATE LOGIN [sql login] WITH PASSWORD =N'Pass''word'", createScript(login));
    }

    @ParameterizedTest
    @EnumSource(value = SQLServerLogin.LoginType.class, names = {"U", "G"})
    public void createsWindowsUserOrGroupWithoutPassword(@NotNull SQLServerLogin.LoginType type) throws DBException {
        SQLServerLogin login = new SQLServerLogin(dataSource, "DOMAIN\\account");
        login.setLoginType(type);

        Assertions.assertEquals("CREATE LOGIN [DOMAIN\\account] FROM WINDOWS", createScript(login));
    }

    @ParameterizedTest
    @EnumSource(value = SQLServerLogin.LoginType.class, names = {"U", "G"})
    public void ignoresPasswordForWindowsPrincipal(@NotNull SQLServerLogin.LoginType type) throws DBException {
        SQLServerLogin login = new SQLServerLogin(dataSource, "DOMAIN\\group]name");
        login.setLoginType(type);
        login.setPassword("unused password");

        Assertions.assertEquals("CREATE LOGIN [DOMAIN\\group]]name] FROM WINDOWS", createScript(login));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void rejectsSqlLoginWithoutPassword(@Nullable String password) {
        SQLServerLogin login = new SQLServerLogin(dataSource, "sql login");
        login.setPassword(password);

        Assertions.assertThrows(DBException.class, () -> createScript(login));
    }

    @ParameterizedTest
    @EnumSource(value = SQLServerLogin.LoginType.class, names = {"R", "C", "E", "X", "K"})
    public void rejectsUnsupportedLoginTypes(@NotNull SQLServerLogin.LoginType type) {
        SQLServerLogin login = new SQLServerLogin(dataSource, "unsupported login");
        login.setLoginType(type);
        login.setPassword("password");

        Assertions.assertThrows(DBException.class, () -> createScript(login));
    }

    @Test
    public void changesDefaultDatabase() throws DBException {
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new SQLServerDialectMssql());
        SQLServerLogin login = new SQLServerLogin(dataSource, "sql login");
        SQLServerDatabase database = Mockito.mock(SQLServerDatabase.class);
        Mockito.when(database.getDataSource()).thenReturn(dataSource);
        Mockito.when(database.getName()).thenReturn("db]name");
        login.setDefaultDatabase(database);
        Assertions.assertEquals("ALTER LOGIN [sql login] WITH DEFAULT_DATABASE = [db]]name]",
            new ModifyManager().modify(login, "defaultDatabase").getFirst().getScript());
    }

    @Test
    public void changesDefaultLanguage() throws DBException {
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new SQLServerDialectMssql());
        SQLServerLogin login = new SQLServerLogin(dataSource, "sql login");
        login.setDefaultLanguageName("British English");
        Assertions.assertEquals("ALTER LOGIN [sql login] WITH DEFAULT_LANGUAGE = [British English]",
            new ModifyManager().modify(login, "defaultLanguageName").getFirst().getScript());
    }

    @Test
    public void rejectsEmptyDefaultDatabase() {
        Assertions.assertThrows(DBException.class, () ->
            new ModifyManager().modify(new SQLServerLogin(dataSource, "sql login"), "defaultDatabase"));
    }

    @Test
    public void rejectsEmptyDefaultLanguage() {
        Assertions.assertThrows(DBException.class, () ->
            new ModifyManager().modify(new SQLServerLogin(dataSource, "sql login"), "defaultLanguageName"));
    }

    private static class ModifyManager extends SQLServerLoginManager {
        @NotNull
        private List<DBEPersistAction> modify(@NotNull SQLServerLogin login, @NotNull String property) throws DBException {
            @SuppressWarnings("unchecked")
            ObjectChangeCommand command = Mockito.mock(ObjectChangeCommand.class);
            Mockito.when(command.getObject()).thenReturn(login);
            Mockito.when(command.hasProperty(property)).thenReturn(true);
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectModifyActions(new VoidProgressMonitor(), Mockito.mock(DBCExecutionContext.class), actions, command, Map.of());
            return actions;
        }
    }

    @NotNull
    private String createScript(@NotNull SQLServerLogin login) throws DBException {
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new SQLServerDialectMssql());
        SQLServerLoginManager.ObjectCreateCommand command = manager.makeCreateCommand(login, Map.of());
        DBEPersistAction[] actions = command.getPersistActions(new VoidProgressMonitor(), executionContext, Map.of());
        Assertions.assertNotNull(actions);
        Assertions.assertEquals(1, actions.length);
        Assertions.assertTrue(command.isDisableSessionLogging());
        return actions[0].getScript();
    }
}
