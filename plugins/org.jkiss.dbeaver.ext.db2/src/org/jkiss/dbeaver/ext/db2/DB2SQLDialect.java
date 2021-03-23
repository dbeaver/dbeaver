/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHexString;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;

/**
 * DB2 SQL dialect
 * 
 * @author Denis Forveille
 * 
 */
public class DB2SQLDialect extends JDBCSQLDialect {

    private static final Log log = Log.getLog(DB2SQLDialect.class);

    public static final String[] EXEC_KEYWORDS = new String[]{"call"};

    private static final String[][] DB2_BEGIN_END_BLOCK = new String[][]{
    };

    private static final boolean LOAD_ROUTINES_FROM_SYSCAT = false;

    public DB2SQLDialect() {
        super("DB2 LUW", "db2_luw");
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        addSQLKeywords(Arrays.asList(DB2Constants.ADVANCED_KEYWORDS));
        addFunctions(Arrays.asList(DB2Constants.ROUTINES));

        turnFunctionIntoKeyword("TRUNCATE");
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode()
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

    @Override
    protected void loadFunctions(JDBCSession session, JDBCDatabaseMetaData metaData, Set<String> allFunctions) throws DBException, SQLException {
        if (LOAD_ROUTINES_FROM_SYSCAT) {
            try (JDBCStatement stmt = session.createStatement()) {
                try (JDBCResultSet dbResult = stmt.executeQuery(
                    "SELECT DISTINCT ROUTINENAME FROM SYSCAT.ROUTINES")) {
                    while (dbResult.next()) {
                        String routineName = dbResult.getString(1);
                        if (CommonUtils.isEmpty(routineName) || !Character.isLetter(routineName.charAt(0))) {
                            continue;
                        }
                        allFunctions.add(routineName);
                    }
                }
            } catch (Throwable e) {
                log.debug("Error loading DB2 functions", e);
            }
        }
        if (allFunctions.isEmpty()) {
            super.loadFunctions(session, metaData, allFunctions);
        }
    }

    @Nullable
    @Override
    public String getDualTableName() {
        return "SYSIBM.SYSDUMMY1";
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return BinaryFormatterHexString.INSTANCE;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return DB2_BEGIN_END_BLOCK;
    }
    
    @Override
    public String getScriptDelimiterRedefiner() {
    	return "DELIMITER";
    }
}
