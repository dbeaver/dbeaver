package org.jkiss.dbeaver.ext.phoenix.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.sql.SQLConstants;


public class PhoenixSQLDialect extends GenericSQLDialect {
	public static final String[] PHOENIXHBASE_RESERVED_KEYWORDS = {
		"UPSERT"
	};

	public PhoenixSQLDialect(GenericDataSource dataSource, JDBCDatabaseMetaData metaData) {
		super(dataSource, metaData);
	}
	
	@Override
    protected void loadStandardKeywords()
    {
    	super.loadStandardKeywords();
    	Set<String> all = new HashSet<>();
        Collections.addAll(all, PHOENIXHBASE_RESERVED_KEYWORDS);
    	addKeywords(all, DBPKeywordType.KEYWORD);
    }
	
	@Override
	public String prepareUpdateStatement(
    		String schemaName, String tableName, String tableAlias,
    		String[] keyColNames, Object[] keyColVals, String[] valColNames)
    {
		// Make query
        StringBuilder query = new StringBuilder();
        StringBuilder valStatement = new StringBuilder();
        query.append("UPSERT INTO ")
        	.append("\"").append(schemaName).append("\".")
        	.append(tableName);
        valStatement.append("VALUES");
        query.append("(");
        valStatement.append("(");
        
 
        for (String valColName : valColNames) {
        	query.append(valColName).append(", ");
        	valStatement.append("?,");
        }
        
        for (int i=0; i<keyColNames.length; i++) {
        	String keyColName = keyColNames[i];
        	query.append(keyColName);
        	valStatement.append("?");
        	if (i < keyColNames.length-1) {
        		query.append(", ");
        		valStatement.append(", ");
        	}
        }
        
        query.append(")\n");
        valStatement.append(")");
        query.append(valStatement.toString());

    	return query.toString();
    }
	
	@Override
	public String prepareDeleteStatement(String schemaName, String tableName, String tableAlias, String[] keyColNames, Object[] keyColVals)
    {
    	// Make query
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ")
        	.append("\"").append(schemaName).append("\".")
        	.append(tableName);
        if (tableAlias != null) {
            query.append(' ').append(tableAlias);
        }
        query.append("\nWHERE "); //$NON-NLS-1$ //$NON-NLS-2$

        boolean hasKey = false;
        for (int i = 0; i < keyColNames.length; i++) {
            if (hasKey) query.append(" AND "); //$NON-NLS-1$
            hasKey = true;
            String keyColName = keyColNames[i];
            Object keyColVal = keyColVals[i];
            query.append(keyColName);
            if (DBUtils.isNullValue(keyColVal)) {
                query.append(" IS NULL"); //$NON-NLS-1$
            } else {
                query.append("=?"); //$NON-NLS-1$
            }
        }
    	return query.toString();
    }
    
    @Override
    public String prepareInsertStatement(String schemaName, String tableName, String[] keyColNames)
    {
    	// Make query
        StringBuilder query = new StringBuilder(200);
        query.append("UPSERT INTO ")
        	.append("\"").append(schemaName).append("\".")
        	.append(tableName).append(" ("); //$NON-NLS-1$ //$NON-NLS-2$

        boolean hasKey = false;
        for (int i = 0; i < keyColNames.length; i++) {
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            query.append(keyColNames[i]);
        }
        query.append(")\nVALUES ("); //$NON-NLS-1$
        hasKey = false;
        for (int i = 0; i < keyColNames.length; i++) {
            if (hasKey) query.append(","); //$NON-NLS-1$
            hasKey = true;
            query.append("?"); //$NON-NLS-1$
        }
        query.append(")"); //$NON-NLS-1$
    	return query.toString();
    }
}
