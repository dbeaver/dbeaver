package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;

import java.util.Arrays;

public class SnowflakeSQLDialect extends GenericSQLDialect {

    public SnowflakeSQLDialect() {
        super("Snowflake");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        addSQLKeywords(
                Arrays.asList(
                        "QUALIFY"
                ));
    }
}
