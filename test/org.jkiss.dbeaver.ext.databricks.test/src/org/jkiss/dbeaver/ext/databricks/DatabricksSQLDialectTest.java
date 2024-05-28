package org.jkiss.dbeaver.ext.databricks;

import org.jkiss.dbeaver.ext.databricks.DatabricksSQLDialect;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DatabricksSQLDialectTest {

    @Test
    public void TestValidIdentifierStart() {
        DatabricksSQLDialect dialect = new DatabricksSQLDialect();
        Assert.assertTrue(dialect.validIdentifierStart("Databricks".charAt(0)));
        Assert.assertTrue(dialect.validIdentifierStart("_Databricks".charAt(0)));
        Assert.assertTrue(dialect.validIdentifierStart("databricks".charAt(0)));
    }

    @Test
    public void TestInvalidIdentifierStart(){
        DatabricksSQLDialect dialect = new DatabricksSQLDialect();
        Assert.assertFalse(dialect.validIdentifierStart("...Databricks".charAt(0)));
        Assert.assertFalse(dialect.validIdentifierStart("$Databricks".charAt(0)));
        Assert.assertFalse(dialect.validIdentifierStart("。Databricks".charAt(0)));
    }

    @Test
    public void TestValidIdentifierPart() {
        DatabricksSQLDialect dialect = new DatabricksSQLDialect();

        Assert.assertTrue(dialect.validIdentifierPart("Databricks".charAt(0),false));
        Assert.assertTrue(dialect.validIdentifierPart("databricks".charAt(0),false));
        Assert.assertTrue(dialect.validIdentifierPart("1databricks".charAt(0),false));
        Assert.assertTrue(dialect.validIdentifierPart("_databricks".charAt(0),false));
    }

    @Test
    public void TestInvalidIdentifierPart() {
        DatabricksSQLDialect dialect = new DatabricksSQLDialect();

        Assert.assertFalse(dialect.validIdentifierPart("&databricks".charAt(0),false));
        Assert.assertFalse(dialect.validIdentifierPart("....databricks".charAt(0),false));
        Assert.assertFalse(dialect.validIdentifierPart("!databricks".charAt(0),false));
    }
}