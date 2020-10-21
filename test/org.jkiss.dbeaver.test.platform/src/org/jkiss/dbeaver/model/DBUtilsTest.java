package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
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
    public void testMainServices() {

/*
        org.eclipse.equinox.launcher.Main.main(new String[] {
            "-product", "org.jkiss.dbeaver.product"}
        );
        System.out.println("OSGI started");
*/
    }

}