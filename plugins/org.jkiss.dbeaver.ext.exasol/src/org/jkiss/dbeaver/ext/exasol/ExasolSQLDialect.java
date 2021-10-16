/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class ExasolSQLDialect extends JDBCSQLDialect {

    private static final Log LOG = Log.getLog(ExasolDataSource.class);
    
    //Exasol does not support prepareCall
    public static final String[] EXEC_KEYWORDS = new String[]{};

    private static final String[] ADVANCED_FUNCTIONS = {
        "BIT_AND",
        "BIT_CHECK",
        "BIT_LENGTH",
        "BIT_NOT",
        "BIT_OR",
        "BIT_SET",
        "BIT_TO_NUM",
        "CONVERT_TZ",
        "DATE_TRUNC",
        "DECODE",
        "EDIT_DISTANCE",
        "GROUPING_ID",
        "HASH_MD5",
        "HASH_SHA",
        "HASH_TIGER",
        "HASHTYPE_MD5",
        "HASHTYPE_SHA",
        "HASHTYPE_TIGER",
        "IS_DATE",
        "IS_TIMESTAMP",
        "IS_BOOLEAN",
        "IS_DSINTERVAL",
        "IS_YMINTERVAL",
        "JSON_EXTRACT",
        "JSON_VALUE",
        "LAG",
        "LAST_VALUE",
        "LEAD",
        "LISTAGG",
        "MUL",
        "NTILE",
        "NULLIFZERO",
        "NVL",
        "RATIO_TO_REPORT",
        "REGR_FUNCTIONS",
        "ROWNUM",
        "ROWID",
        "SESSION_PARAMETER",
        "ST_BOUNDARY",
        "ST_BUFFER",
        "ST_CENTROID",
        "ST_CONTAINS",
        "ST_CONVEXHULL",
        "ST_CROSSES",
        "ST_DIFFERENCE",
        "ST_DIMENSION",
        "ST_DISJOINT",
        "ST_DISTANCE",
        "ST_ENDPOINT",
        "ST_ENVELOPE",
        "ST_EQUALS",
        "ST_EXTERIORRING",
        "ST_FORCE2D",
        "ST_GEOMETRYN",
        "ST_GEOMETRYTYPE",
        "ST_ISEMPTY",
        "ST_ISRING",
        "ST_ISSIMPLE",
        "ST_LENGTH",
        "ST_MAX_DECIMAL_DIGITS",
        "ST_NUMGEOMETRIES",
        "ST_NUMINTERIORRINGS",
        "ST_NUMPOINTS",
        "ST_OVERLAPS",
        "ST_POINTN",
        "ST_SETSRID",
        "ST_STARTPOINT",
        "ST_SYMDIFFERENCE",
        "ST_TOUCHES",
        "ST_TRANSFORM",
        "ST_UNION",
        "ST_WITHIN",
        "ST_X",
        "ST_Y",
        "SYS_CONNECT_BY_PATH",
        "SYS_GUID",
        "ZEROIFNULL"
    };

    public ExasolSQLDialect() {
        super("Exasol", "exasol");
    }
    
    private void addExtraFunctions(String... functions) {
        super.addFunctions(Arrays.asList(functions));
    }
    
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        
        Collections.addAll(tableQueryWords, "DESC");

        addFunctions(Arrays.asList(ADVANCED_FUNCTIONS));
        
        try {
			try (JDBCStatement stmt = session.createStatement()) {
        		try (JDBCResultSet dbResult = stmt.executeQuery("/*snapshot execution*/ SELECT \"VALUE\" FROM \"$ODBCJDBC\".DB_METADATA WHERE name = 'aggregateFunctions'")) {
        			if (dbResult != null && dbResult.next()) {
        				String keyWord = dbResult.getString(1);
        				
        				String[] aggregateFunctions = keyWord.split(",");
        				this.addExtraFunctions(aggregateFunctions);
        				
        			}
        		}
        		try (JDBCResultSet dbResult = stmt.executeQuery("/*snapshot execution*/ SELECT keyword FROM sys.EXA_SQL_KEYWORDS esk WHERE RESERVED")) {
        			while(dbResult != null && dbResult.next()) {
        				String keyWord = dbResult.getString("KEYWORD");
        				super.addSQLKeyword(keyWord);
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
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
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

    @NotNull
    @Override
    public String escapeScriptValue(DBSTypedObject attribute, @NotNull Object value, @NotNull String strValue) {
		return super.escapeScriptValue(attribute, value, strValue);
    }

    @Override
    public boolean supportsInsertAllDefaultValuesStatement() {
        return true;
    }

}

