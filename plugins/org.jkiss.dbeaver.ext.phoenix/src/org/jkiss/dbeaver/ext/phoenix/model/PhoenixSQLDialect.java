package org.jkiss.dbeaver.ext.phoenix.model;

import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.sql.SQLDialect;

public class PhoenixSQLDialect extends GenericSQLDialect {

	public PhoenixSQLDialect(GenericDataSource dataSource, JDBCDatabaseMetaData metaData) {
		super(dataSource, metaData);
		schemaUsage = SQLDialect.USAGE_ALL;
	}
	
	

}
