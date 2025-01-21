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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;

public class DatabaseURLTest extends DBeaverUnitTest {
    @Test
    public void testMatchPattern() {
        assertMatches(
            "jdbc:postgresql://{host}[:{port}]/[{database}]",
            "jdbc:postgresql://localhost:5432/dvdrental",
            new String[][]{
                {"host", "localhost"},
                {"port", "5432"},
                {"database", "dvdrental"}
            });

        assertMatches(
            "jdbc:teradata://{host}/DATABASE={database},DBS_PORT={port}",
            "jdbc:teradata://localhost/DATABASE=test,DBS_PORT=1234",
            new String[][]{
                {"host", "localhost"},
                {"database", "test"},
                {"port", "1234"}
            });

        assertMatches(
            "jdbc:oracle:thin:@{host}[:{port}]/{database}",
            "jdbc:oracle:thin:@localhost/orcl",
            new String[][]{
                {"host", "localhost"},
                {"database", "orcl"}
            });

        assertMatches(
            "jdbc:sqlserver://{host}[:{port}][;databaseName={database}]",
            "jdbc:sqlserver://localhost:1433;databaseName=master",
            new String[][]{
                {"host", "localhost"},
                {"port", "1433"},
                {"database", "master"}
            });

        assertMatches(
            "jdbc:sqlserver://{host}[:{port}][;databaseName={database}]",
            "jdbc:sqlserver://localhost",
            new String[][]{
                {"host", "localhost"}
            });

        assertMatches(
            "jdbc:sqlite:{file}",
            "jdbc:sqlite:C:\\Users\\%USERNAME%\\Documents\\Chinook.db",
            new String[][]{
                {"file", "C:\\Users\\%USERNAME%\\Documents\\Chinook.db"}
            });

        assertFind(
            "jdbc:mysql://{host}[:{port}]/[{database}]",
            "jdbc:mysql://mysql-rfam-public.ebi.ac.uk:4497/Rfam?useSSL=false&serverTimezone=UTC",
            new String[][]{
                {"host", "mysql-rfam-public.ebi.ac.uk"},
                {"port", "4497"},
                {"database", "Rfam"}
            });
    }

    private void assertMatches(@NotNull String sampleUrl, @NotNull String targetUrl, @NotNull String[][] properties) {
        final Matcher matcher = DatabaseURL.getPattern(sampleUrl).matcher(targetUrl);
        Assert.assertTrue(sampleUrl, matcher.matches());
        for (String[] property : properties) {
            Assert.assertEquals(sampleUrl, property[1], matcher.group(property[0]));
        }
    }

    private void assertFind(@NotNull String sampleUrl, @NotNull String targetUrl, @NotNull String[][] properties) {
        final Matcher matcher = DatabaseURL.getPattern(sampleUrl).matcher(targetUrl);
        Assert.assertTrue(sampleUrl, matcher.find());
        for (String[] property : properties) {
            Assert.assertEquals(sampleUrl, property[1], matcher.group(property[0]));
        }
    }
}
