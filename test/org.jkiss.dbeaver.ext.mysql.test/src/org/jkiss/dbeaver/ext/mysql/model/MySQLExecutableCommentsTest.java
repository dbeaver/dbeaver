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

import net.sf.jsqlparser.util.TablesNamesFinder;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MySQLExecutableCommentsTest {
    @Test
    public void versionHeaderMatchesMySqlLexer() throws Exception {
        String withoutSeparator = "SELECT /*!500001+(SELECT id FROM secret),*/0";
        for (String serverVersion : new String[]{"8.0.36", "8.4.0"}) {
            MySQLDialect dialect = dialect(false, serverVersion);
            Assertions.assertTrue(new TablesNamesFinder<>().getTables(
                SQLSemanticProcessor.parseQuery(dialect, withoutSeparator)).contains("secret"), serverVersion);
        }
        String withSeparator = "SELECT /*!500001 +(SELECT id FROM secret),*/0";
        Assertions.assertTrue(new TablesNamesFinder<>().getTables(
            SQLSemanticProcessor.parseQuery(dialect(false, "8.0.36"), withSeparator)).contains("secret"));
        Assertions.assertTrue(new TablesNamesFinder<>().getTables(
            SQLSemanticProcessor.parseQuery(dialect(false, "8.4.0"), withSeparator)).isEmpty());
    }

    @Test
    public void evaluatesMySqlVersionBoundariesAndMariaDbOnlyComments() {
        MySQLDialect dialect = dialect(false, "8.0.36");
        Assertions.assertEquals(Boolean.TRUE, dialect.isExecutableCommentActive("/*!", 0));
        Assertions.assertEquals(Boolean.TRUE, dialect.isExecutableCommentActive("/*!", 80036));
        Assertions.assertEquals(Boolean.FALSE, dialect.isExecutableCommentActive("/*!", 80037));
        Assertions.assertEquals(Boolean.FALSE, dialect.isExecutableCommentActive("/*M!", 0));
    }

    @Test
    public void evaluatesMariaDbVersionBoundariesAndMySqlExclusions() {
        for (String version : new String[]{"10.9.8-MariaDB", "5.5.5-10.9.8-MariaDB"}) {
            MySQLDialect dialect = dialect(true, version);
            Assertions.assertEquals(Boolean.TRUE, dialect.isExecutableCommentActive("/*!", 50699));
            Assertions.assertEquals(Boolean.FALSE, dialect.isExecutableCommentActive("/*!", 50700));
            Assertions.assertEquals(Boolean.FALSE, dialect.isExecutableCommentActive("/*!", 99999));
            Assertions.assertEquals(Boolean.TRUE, dialect.isExecutableCommentActive("/*M!", 50700));
            Assertions.assertEquals(Boolean.TRUE, dialect.isExecutableCommentActive("/*M!", 100908));
            Assertions.assertEquals(Boolean.FALSE, dialect.isExecutableCommentActive("/*M!", 100909));
        }
    }

    @Test
    public void leavesVersionDependentCommentsUnresolvedWithoutServerMetadata() {
        MySQLDialect dialect = new MySQLDialect();
        Assertions.assertEquals(Boolean.TRUE, dialect.isExecutableCommentActive("/*!", 0));
        Assertions.assertNull(dialect.isExecutableCommentActive("/*!", 80000));
        Assertions.assertNull(dialect.isExecutableCommentActive("/*M!", 0));
        Assertions.assertNull(dialect(false, "unknown").isExecutableCommentActive("/*!", 80000));
    }

    @Test
    public void serverFlavorDoesNotDependOnDriver() {
        Assertions.assertEquals(Boolean.TRUE,
            dialect(false, "MySQL", "10.9.8-MariaDB").isExecutableCommentActive("/*M!", 0));
        Assertions.assertEquals(Boolean.FALSE,
            dialect(false, "MariaDB", "10.9.8").isExecutableCommentActive("/*!", 50700));
        Assertions.assertNull(dialect(false, "unknown", "8.0.36").isExecutableCommentActive("/*M!", 0));
        Assertions.assertNull(dialect(false, "MySQL", "10.9.8").isExecutableCommentActive("/*M!", 0));
        Assertions.assertEquals(Boolean.TRUE,
            dialect(false, "MySQL", "26.7.0").isExecutableCommentActive("/*!", 260700));
        Assertions.assertEquals(Boolean.FALSE,
            dialect(false, "MySQL", "26.7.0").isExecutableCommentActive("/*M!", 0));
    }

    private MySQLDialect dialect(boolean mariaDb, String version) {
        return dialect(mariaDb, mariaDb ? "MariaDB" : "MySQL", version);
    }

    private MySQLDialect dialect(boolean mariaDb, String product, String version) {
        MySQLDataSource dataSource = Mockito.mock(MySQLDataSource.class, Mockito.CALLS_REAL_METHODS);
        DBPDataSourceInfo info = Mockito.mock(DBPDataSourceInfo.class);
        Mockito.doReturn(info).when(dataSource).getInfo();
        Mockito.when(info.getDatabaseProductName()).thenReturn(product);
        Mockito.when(info.getDatabaseProductVersion()).thenReturn(version);
        Mockito.doReturn(mariaDb).when(dataSource).isMariaDB();
        MySQLDialect dialect = new MySQLDialect();
        dialect.afterDataSourceInitialization(dataSource);
        // validation must use the metadata snapshot without returning to the datasource
        Mockito.reset(dataSource);
        return dialect;
    }
}
