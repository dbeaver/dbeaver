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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MySQLViewTest extends DBeaverUnitTest {

    @Test
    public void getViewOptionsClauseWhenDefinerAndSqlSecurityThenAllOptionsKept() {
        String clause = MySQLView.getViewOptionsClause(
            "CREATE ALGORITHM=UNDEFINED DEFINER=`view_owner`@`%` SQL SECURITY INVOKER");
        Assertions.assertEquals(" ALGORITHM=UNDEFINED  DEFINER=`view_owner`@`%`  SQL SECURITY INVOKER ", clause);
    }

    @Test
    public void getViewOptionsClauseWhenSqlSecurityIsLastOptionThenItIsKept() {
        String clause = MySQLView.getViewOptionsClause("CREATE SQL SECURITY DEFINER");
        Assertions.assertEquals(" SQL SECURITY DEFINER ", clause);
    }

    @Test
    public void getViewOptionsClauseWhenDefinerHasSpecialCharactersThenDefinerKeptAsIs() {
        String clause = MySQLView.getViewOptionsClause(
            "CREATE ALGORITHM=MERGE DEFINER=`app ``user`@`10.0.%` SQL SECURITY DEFINER");
        Assertions.assertEquals(" ALGORITHM=MERGE  DEFINER=`app ``user`@`10.0.%`  SQL SECURITY DEFINER ", clause);
    }

    @Test
    public void getViewOptionsClauseWhenDefinerIsRoleThenDefinerKept() {
        // MariaDB allows a role as definer
        String clause = MySQLView.getViewOptionsClause("CREATE DEFINER=`app_role` SQL SECURITY DEFINER");
        Assertions.assertEquals(" DEFINER=`app_role`  SQL SECURITY DEFINER ", clause);
    }

    @Test
    public void getViewOptionsClauseWhenNoOptionsThenEmpty() {
        Assertions.assertEquals("", MySQLView.getViewOptionsClause("CREATE"));
    }
}
