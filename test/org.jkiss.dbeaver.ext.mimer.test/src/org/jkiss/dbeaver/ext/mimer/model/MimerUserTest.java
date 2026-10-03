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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

/**
 * {@link MimerUser}'s DDL builders - {@code CREATE}/{@code DROP IDENT}, the per-group {@code
 * GRANT MEMBER} statements a new user's Groups list produces, and the password-only {@code ALTER
 * IDENT} (OS login authorizations are a separate class, {@link MimerUserAuthorization} - not
 * covered here).
 *
 * @author Mimer Information Technology
 */
public class MimerUserTest extends DBeaverUnitTest {

    @Mock
    private MimerDataSource dataSource;

    @Test
    public void createDDLDefaultsToWithSchemaAndNoPassword() {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        Assertions.assertEquals("CREATE IDENT \"fredrik\" AS USER", user.buildCreateDDL());
    }

    @Test
    public void createDDLIncludesAPasswordWhenSet() {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        user.setPassword("secret");
        Assertions.assertEquals("CREATE IDENT \"fredrik\" AS USER USING 'secret'", user.buildCreateDDL());
    }

    @Test
    public void createDDLEscapesASingleQuoteInThePassword() {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        user.setPassword("o'brien");
        Assertions.assertEquals("CREATE IDENT \"fredrik\" AS USER USING 'o''brien'", user.buildCreateDDL());
    }

    @Test
    public void createDDLOnlyEmitsWithoutSchemaToOptOutOfTheDefault() {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        user.setWithoutSchema(true);
        Assertions.assertEquals("CREATE IDENT \"fredrik\" AS USER WITHOUT SCHEMA", user.buildCreateDDL());
    }

    @Test
    public void createDDLCombinesPasswordAndWithoutSchema() {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        user.setPassword("secret");
        user.setWithoutSchema(true);
        Assertions.assertEquals("CREATE IDENT \"fredrik\" AS USER USING 'secret' WITHOUT SCHEMA", user.buildCreateDDL());
    }

    @Test
    public void dropDDLIsAPlainDropWithNoCascadeOption() {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        Assertions.assertEquals("DROP IDENT \"fredrik\"", user.buildDropDDL());
    }

    @Test
    public void groupGrantDDLIsEmptyWithNoInitialGroups() {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        Assertions.assertTrue(user.buildGroupGrantDDL().isEmpty());
    }

    @Test
    public void groupGrantDDLEmitsOneGrantMemberStatementPerInitialGroup() {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        user.setInitialGroups(List.of("developers", "admins"));

        List<String> statements = user.buildGroupGrantDDL();

        Assertions.assertEquals(List.of(
            "GRANT MEMBER ON GROUP \"developers\" TO \"fredrik\"",
            "GRANT MEMBER ON GROUP \"admins\" TO \"fredrik\""
        ), statements);
    }

    @Test
    public void alterDDLIsEmptyWhenPasswordWasNotChanged() {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        user.setPassword("secret"); // set, but not marked changed below
        Assertions.assertTrue(user.buildAlterDDL(Set.of()).isEmpty());
    }

    @Test
    public void alterDDLSetsThePasswordWhenChanged() {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        user.setPassword("newpass");

        List<String> statements = user.buildAlterDDL(Set.of("password"));

        Assertions.assertEquals(List.of("ALTER IDENT \"fredrik\" SET PASSWORD 'newpass'"), statements);
    }

    @Test
    public void alterDDLIgnoresAPasswordChangeMarkedButClearedToBlank() {
        MimerUser user = new MimerUser(dataSource, "fredrik");
        user.setPassword(""); // cleared - never a valid ALTER IDENT SET PASSWORD ''
        Assertions.assertTrue(user.buildAlterDDL(Set.of("password")).isEmpty());
    }
}
