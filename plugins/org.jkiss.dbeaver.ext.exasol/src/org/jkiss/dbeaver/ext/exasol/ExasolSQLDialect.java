package org.jkiss.dbeaver.ext.exasol;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;

public class ExasolSQLDialect extends JDBCSQLDialect {
	
    private static final Log LOG = Log.getLog(ExasolDataSource.class);
    public static final String[] EXEC_KEYWORDS = new String[]{"execute script"};


	    public ExasolSQLDialect(JDBCDatabaseMetaData metaData)  
	    {
	        super("Exasol", metaData);
	        try {
		        for (String kw : metaData.getSQLKeywords().split(",")) {
		            this.addSQLKeyword(kw);
		        }
	        } catch (SQLException e)
	        {
	        	LOG.warn("Could not retrieve reserved keyword list from Exasol dictionary");
	        }
	        
	    }

	    @NotNull
	    @Override
	    public MultiValueInsertMode getMultiValueInsertMode()
	    {
	        return MultiValueInsertMode.GROUP_ROWS;
	    }

	    @Override
	    public boolean supportsAliasInSelect() {
	        return true;
	    }

	    @NotNull
	    @Override
	    public String[] getExecuteKeywords()
	    {
	        return EXEC_KEYWORDS;
	    }

	}

