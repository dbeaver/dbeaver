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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.parser.QueryReconstructor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QueryReconstructorTest extends DBeaverUnitTest {

    @Test
    public void insertingFragmentBetweenComments() throws Exception {

        // GIVEN
        String originalSql = String.join(
            "\n",
            "/* lead */",
            "SELECT id",
            "FROM t1 -- inner",
            "JOIN t2 ON t1.id = t2.id",
            "ORDER BY id",
            "/* trail */"
        );
        String newSqlWithoutComments = String.join(
            "\n",
            "SELECT id",
            "FROM t1",
            "JOIN t2 ON t1.id = t2.id",
            "JOIN t3 ON t1.id = t3.id",
            "ORDER BY id"
        );

        // WHEN
        QueryReconstructor queryReconstructor = new QueryReconstructor(BasicSQLDialect.INSTANCE, originalSql);
        String reconstructedText = queryReconstructor.reconstructFromOriginalFragments(newSqlWithoutComments);

        //THEN
        String expectedText = String.join(
            "\n",
            "/* lead */",
            "SELECT id",
            "FROM t1 -- inner",
            "JOIN t2 ON t1.id = t2.id",
            "JOIN t3 ON t1.id = t3.id",
            "ORDER BY id",
            "/* trail */"
        );
        Assertions.assertEquals(expectedText, reconstructedText);
    }

    @Test
    public void replacingSelectAllWithColumns() throws Exception {

        // GIVEN
        String originalSql = String.join(
            "\n",
            "SELECT",
            "\t*",
            "FROM t -- c",
            "ORDER BY x"
        );
        String newSqlWithoutComments = String.join(
            "\n",
            "SELECT",
            "\tt.a,",
            "\tt.b",
            "FROM t",
            "ORDER BY x"
        );

        // WHEN
        QueryReconstructor queryReconstructor = new QueryReconstructor(BasicSQLDialect.INSTANCE, originalSql);
        String reconstructedText = queryReconstructor.reconstructFromOriginalFragments(newSqlWithoutComments);

        // THEN
        String expectedText = String.join(
            "\n",
            "SELECT",
            "\tt.a,",
            "\tt.b",
            "FROM t -- c",
            "ORDER BY x"
        );
        Assertions.assertEquals(expectedText, reconstructedText);
    }

    @Test
    public void trailingOnlyComment() throws Exception {

        // GIVEN
        String originalSql = "SELECT * FROM tablename --comment";
        String newSqlWithoutComments = "SELECT col FROM tablename";

        // WHEN
        QueryReconstructor queryReconstructor = new QueryReconstructor(BasicSQLDialect.INSTANCE, originalSql);
        String reconstructedText = queryReconstructor.reconstructFromOriginalFragments(newSqlWithoutComments);

        // THEN
        Assertions.assertEquals("SELECT col FROM tablename --comment", reconstructedText);
    }
}
