/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.utils.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DBUtilsTest {

    BasicSQLDialect sqlDialect = new BasicSQLDialect() {
        @NotNull
        @Override
        public String[][] getIdentifierQuoteStrings() {
            return new String[][] { { "\"", "\""} };
        }

        @NotNull
        @Override
        public DBPIdentifierCase storesUnquotedCase() {
            return DBPIdentifierCase.LOWER;
        }

        @NotNull
        @Override
        public DBPIdentifierCase storesQuotedCase() {
            return DBPIdentifierCase.MIXED;
        }
    };
    @Mock
    JDBCDataSource mockDataSource;

    @Before
    public void setUp() throws Exception {
        Mockito.when(mockDataSource.getSQLDialect()).thenReturn(sqlDialect);
    }

    @Test
    public void checkIdentifiersQuote() throws Exception {
        Assert.assertEquals(DBUtils.getQuotedIdentifier(mockDataSource, "table_name"), "table_name");
        Assert.assertEquals(DBUtils.getQuotedIdentifier(mockDataSource, "table name"), "\"table name\"");
        Assert.assertEquals(DBUtils.getQuotedIdentifier(mockDataSource, "TableName"), "\"TableName\"");
    }

    @Test
    public void testExtractTypeModifiers() throws DBException {
        Assert.assertEquals(new Pair<>("NUMBER", new String[0]), DBUtils.getTypeModifiers("NUMBER"));
        Assert.assertEquals(new Pair<>("NUMBER", new String[]{"5"}), DBUtils.getTypeModifiers("NUMBER(5)"));
        Assert.assertEquals(new Pair<>("NUMBER", new String[]{"10", "5"}), DBUtils.getTypeModifiers("NUMBER(10,   5)"));
        Assert.assertEquals(new Pair<>("NUMBER", new String[]{"10", "5", "TEST"}), DBUtils.getTypeModifiers("NUMBER (10, 5, TEST)"));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("NUMBER()"));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("NUMBER("));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("NUMBER)"));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("NUMBER(5, 10"));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("(5, 10)"));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("()"));
    }

    @Test
    public void testMainServices() {

/*
        org.eclipse.equinox.launcher.Main.main(new String[] {
            "-product", "org.jkiss.dbeaver.product"}
        );
        System.out.println("OSGI started");
*/
    }

}