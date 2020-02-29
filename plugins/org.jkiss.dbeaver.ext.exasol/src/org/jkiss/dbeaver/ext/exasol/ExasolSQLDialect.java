/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;

public class ExasolSQLDialect extends JDBCSQLDialect {

    private static final Log LOG = Log.getLog(ExasolDataSource.class);
    
    //Exasol does not support prepareCall
    public static final String[] EXEC_KEYWORDS = new String[]{};


    public ExasolSQLDialect() {
        super("Exasol");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        
        
        try {
            for (String kw : metaData.getSQLKeywords().split(",")) {
                this.addSQLKeyword(kw);
            } 
            
        	JDBCSession session = DBUtils.openMetaSession(new VoidProgressMonitor(), dataSource, "" );
        	try (JDBCStatement stmt = session.createStatement())
        	{
        		try (JDBCResultSet dbResult = stmt.executeQuery("SELECT KEYWORD,RESERVED FROM  EXA_SQL_KEYWORDS")) 
        		{
        			
        			while(dbResult.next())
        			{
        				Boolean isReserved = dbResult.getBoolean(2);
        				String keyWord = dbResult.getString(1);
        				DBPKeywordType type = DBPKeywordType.OTHER;
        				if (isReserved)
        					type = DBPKeywordType.KEYWORD;
        				
        				if (  
        					! (this.getMatchedKeywords(keyWord).stream().anyMatch(k -> k.equals(keyWord)))
        				) {
        					@SuppressWarnings("serial")
							ArrayList<String> value = new ArrayList<String>() {{
    							add(keyWord);
    						}};
        					this.addKeywords(value, type);;
        				}
        			}
        		}
        	}
        } catch (SQLException e) {
            LOG.warn("Could not retrieve reserved keyword list from Exasol dictionary");
        }
        
		@SuppressWarnings("serial")
		ArrayList<String> value = new ArrayList<String>() {{
			add("KERBEROS");
			add("JDBC");
		}};
		
		this.addKeywords(value, DBPKeywordType.OTHER);
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return new String[]{};
    }

}

