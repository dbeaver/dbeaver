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
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class ExasolSQLDialect extends JDBCSQLDialect {

    private static final Log LOG = Log.getLog(ExasolDataSource.class);
    
    //Exasol does not support prepareCall
    public static final String[] EXEC_KEYWORDS = new String[]{};


    public ExasolSQLDialect() {
        super("Exasol");
    }
    
    public void addExtraFunctions(String... functions) {
        super.addFunctions(Arrays.asList(functions));
    }


    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        
        Collections.addAll(tableQueryWords, "DESC");
        
        try {
          
        	JDBCSession session = DBUtils.openMetaSession(new VoidProgressMonitor(), dataSource, "" );
        	try (JDBCStatement stmt = session.createStatement())
        	{
        		try (JDBCResultSet dbResult = stmt.executeQuery("/*snapshot execution*/ SELECT \"VALUE\" FROM \"$ODBCJDBC\".DB_METADATA WHERE name = 'aggregateFunctions'")) 
        		{
        			
        			if (dbResult.next())
        			{
        				String keyWord = dbResult.getString(1);
        				
        				String[] aggregateFunctions = keyWord.split(",");
        				this.addExtraFunctions(aggregateFunctions);
        				
        			}
        		}
        	}
        } catch (SQLException e) {
            LOG.warn("Could not retrieve functions list from Exasol dictionary");
        }
        
		@SuppressWarnings("serial")
		ArrayList<String> value = new ArrayList<String>() {{
			add("KERBEROS");
			add("JDBC");
			add("BYTE");
			add("BIT");
			add("PRECEDENCE");
			add("GROUP_TEMP_DB_RAM_LIMIT");
			add("USER_TEMP_DB_RAM_LIMIT");
			add("SESSION_TEMP_DB_RAM_LIMIT");
			add("CPU_WEIGHT");
		}};
		
		this.addKeywords(value, DBPKeywordType.KEYWORD);
		
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
        return new String[]{"EXECUTE SCRIPT"};
    }

    @Override
    public String escapeScriptValue(DBSAttributeBase attribute, Object value, String strValue) {
		return super.escapeScriptValue(attribute, value, strValue);
    }

}

