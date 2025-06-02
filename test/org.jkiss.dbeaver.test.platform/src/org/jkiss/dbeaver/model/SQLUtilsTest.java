/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class SQLUtilsTest extends DBeaverUnitTest {
    @Test
    public void makeRegexFromLikeTest() {
        Assert.assertEquals("^ABC$", SQLUtils.makeRegexFromLike("ABC"));
        Assert.assertEquals("^A.*C$", SQLUtils.makeRegexFromLike("A%C"));
        Assert.assertEquals("^ABC", SQLUtils.makeRegexFromLike("ABC%"));
        Assert.assertEquals("ABC$", SQLUtils.makeRegexFromLike("%ABC"));
        Assert.assertEquals("ABC", SQLUtils.makeRegexFromLike("%ABC%"));
        Assert.assertEquals("^A.C$", SQLUtils.makeRegexFromLike("A_C"));
        Assert.assertEquals("A.C", SQLUtils.makeRegexFromLike("%A_C%"));
    }
    
    @Test
    public void fixLineFeedsTest() {
        Assert.assertEquals(
            "SELECT LastName -- x\r\n"
            + "FROM Persons drai where PersonID  = 1\r\n"
            + "-- AND ResourceId  = 1\n\r"
            + "ORDER BY PersonID ;",
            SQLUtils.fixLineFeeds("SELECT LastName -- x\r"
            + "FROM Persons drai where PersonID  = 1\r\n"
            + "-- AND ResourceId  = 1\n\r"
            + "ORDER BY PersonID ;"));
    }

    @Test
    public void makeGlobFromSqlLikePattern_whenWithNoSpecialSymbols_thenSuccess(){

        String source = "key1234";
        Assert.assertEquals(source, SQLUtils.makeGlobFromSqlLikePattern(source));
    }

    @Test
    public void makeGlobFromSqlLikePattern_whenWithSpecialSymbols_thenSuccess(){

        Assert.assertEquals("key?*\\?*\\", SQLUtils.makeGlobFromSqlLikePattern("key_%?*\\"));
    }

    @Test
    public void stripTrailingCommentsTest() {
        final String input ="""
-- DROP TABLE public.books;

CREATE TABLE public.books (
    id_book int4 DEFAULT nextval('book_seq'::regclass) NOT NULL,
    id_series int4 NULL,
    file_name varchar(250) NOT NULL, -- File Name
    comment varchar(250) DEFAULT '<-- empty -->' NOT NULL -- Second single line comment
);
        """;
        final String expected = """
-- DROP TABLE public.books;

CREATE TABLE public.books (
    id_book int4 DEFAULT nextval('book_seq'::regclass) NOT NULL,
    id_series int4 NULL,
    file_name varchar(250) NOT NULL,
    comment varchar(250) DEFAULT '<-- empty -->' NOT NULL
);""";

        PostgreDialect dialect = new PostgreDialect();
        String[] slm = dialect.getSingleLineComments();
        String ls = GeneralUtils.getDefaultLineSeparator();

        Assert.assertEquals(expected, SQLUtils.stripTrailingComments(input, slm, ls));
    }

    @Test
    public void compactSQLTest() {
        PostgreDialect postgreDialect = new PostgreDialect();

        final String inputCompactTest = """
/*
 Multiline comment 1
*/

-- public.books definition

-- Drop table

-- DROP TABLE public.books;

CREATE TABLE public.books (
    id_book int4 DEFAULT nextval('book_seq'::regclass) NOT NULL,
    id_series int4 NULL,
    file_name varchar(250) NOT NULL, -- File Name
    comment varchar(250) DEFAULT '<-- empty -->' NOT NULL -- Second single line comment
);

CREATE UNIQUE INDEX books_hash_name_idx
    ON public.books USING btree (hash_name);
CREATE INDEX idx_books_free ON public.books USING btree (free);

-- public.books foreign keys

 ALTER TABLE public.books
    ADD CONSTRAINT books_id_series_fkey FOREIGN KEY (id_series) REFERENCES public.series(id_series);

            """;

        final String expectedCompactTest = """
-- public.books definition

-- Drop table

-- DROP TABLE public.books;

CREATE TABLE public.books ( id_book int4 DEFAULT nextval('book_seq'::regclass) NOT NULL, id_series int4 NULL, file_name varchar(250) NOT NULL, comment varchar(250) DEFAULT '<-- empty -->' NOT NULL );

CREATE UNIQUE INDEX books_hash_name_idx ON public.books USING btree (hash_name);
CREATE INDEX idx_books_free ON public.books USING btree (free);

-- public.books foreign keys

ALTER TABLE public.books ADD CONSTRAINT books_id_series_fkey FOREIGN KEY (id_series) REFERENCES public.series(id_series);""";

        Assert.assertEquals(expectedCompactTest, SQLUtils.compact(inputCompactTest, postgreDialect));
    }

}
