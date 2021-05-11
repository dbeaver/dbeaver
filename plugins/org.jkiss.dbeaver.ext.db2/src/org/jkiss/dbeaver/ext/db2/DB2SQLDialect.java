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
import org.jkiss.dbeaver.ext.db2.model.DB2Routine;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHexString;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLMultiWordRule;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * DB2 SQL dialect
 * 
 * @author Denis Forveille
 * 
 */
public class DB2SQLDialect extends JDBCSQLDialect implements TPRuleProvider {

    private static final Log log = Log.getLog(DB2SQLDialect.class);

    public static final String[] EXEC_KEYWORDS = new String[]{"CALL"};

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

    @Override
    protected String getStoredProcedureCallInitialClause(DBSProcedure proc) {
        if (proc.getProcedureType() == DBSProcedureType.FUNCTION && proc instanceof DB2Routine && ((DB2Routine) proc).getFunctionType() == DB2Routine.FunctionType.T) {
            // Function call is special for table functions
            return "SELECT * FROM TABLE (" + proc.getFullyQualifiedName(DBPEvaluationContext.DML);
        }
        return super.getStoredProcedureCallInitialClause(proc);
    }

    @NotNull
    @Override
    protected String getProcedureCallEndClause(DBSProcedure procedure) {
        if (procedure.getProcedureType() == DBSProcedureType.FUNCTION) {
            // Only "Select function_name" doesn't work for user-defined DB2 functions. See #10059
            if (procedure instanceof DB2Routine && ((DB2Routine) procedure).getFunctionType() == DB2Routine.FunctionType.T) {
                // Start is in getStoredProcedureCallInitialClause
                return ")";
            }
            // This part necessary for scalar functions
            return "FROM SYSIBM.SYSDUMMY1";
        }
        return super.getProcedureCallEndClause(procedure);
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
    public String getScriptDelimiterRedefiner() {
    	return "DELIMITER";
    }

    @Override
    public void extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull List<TPRule> rules, @NotNull RulePosition position) {
        if (position == RulePosition.KEYWORDS) {
            final TPTokenDefault keywordToken = new TPTokenDefault(SQLTokenType.T_KEYWORD);
            rules.add(new SQLMultiWordRule(new String[]{"ROW", "BEGIN"}, keywordToken));
            rules.add(new SQLMultiWordRule(new String[]{"ROW", "END"}, keywordToken));
        }
    }
}
