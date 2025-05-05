/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.parser;

import org.eclipse.jface.text.Document;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.rules.ScriptParameterRule;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.util.SQLEditorTestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class SQLScriptParserGenericsTest extends DBeaverUnitTest {
    @Mock
    private DBPDriver driver;
    @Mock
    private GenericDataSource dataSource;
    @Mock
    private GenericMetaModel metaModel;
    @Mock
    private DBPDataSourceContainer dataSourceContainer;
    @Mock
    private DBCExecutionContext executionContext;
    @Mock
    private JDBCSession session;
    @Mock
    private JDBCDatabaseMetaData databaseMetaData;

    @Before
    public void init() {
        DBPConnectionConfiguration connectionConfiguration = new DBPConnectionConfiguration();
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        Mockito.when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        Mockito.lenient().when(dataSourceContainer.getConnectionConfiguration()).thenReturn(connectionConfiguration);
        Mockito.lenient().when(dataSourceContainer.getActualConnectionConfiguration()).thenReturn(connectionConfiguration);
        Mockito.when(dataSourceContainer.getPreferenceStore()).thenReturn(preferenceStore);
        Mockito.when(dataSourceContainer.getDriver()).thenReturn(driver);
        Mockito.lenient().when(executionContext.getDataSource()).thenReturn(dataSource);
        Mockito.when(driver.getDriverParameter(Mockito.anyString())).thenReturn(null);
        Mockito.when(dataSource.getMetaModel()).thenReturn(metaModel);
        Mockito.when(metaModel.supportsUpsertStatement()).thenReturn(false);
    }

    @Test
    public void parseBeginTransaction() throws DBException {
        assertParse("snowflake",
            "begin transaction;\nselect 1 from dual;",
            new String[]{"begin transaction", "select 1 from dual"}
        );
    }

    @Test
    public void parseFromCursorPositionBeginTransaction() throws DBException {
        String query = """
            begi<-|n transaction;<-|
            select 1 from dual;
            """;
        var modifiedQueryAndPositions = SQLEditorTestUtil.getCursorPositions(query);
        String modifiedQuery = modifiedQueryAndPositions.keySet().iterator().next();
        int[] positions = modifiedQueryAndPositions.get(modifiedQuery);
        SQLScriptElement element;
        SQLParserContext context = createParserContext(setDialect("snowflake"), modifiedQuery);
        for (int pos : positions) {
            element = SQLScriptParser.parseQuery(context, 0, modifiedQuery.length(), pos, false, false);
            Assert.assertEquals("begin transaction", element.getText());
        }
    }

    @Test
    public void parseQueryBeforeBlockDeclaration() throws DBException {
        SQLDialect hanaDialect = setDialect("sap_hana");
        Assert.assertTrue(hanaDialect.isStripCommentsBeforeBlocks());
        {
            String query = "/* Issue */\n" + "DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END;";
            SQLParserContext context = createParserContext(hanaDialect, query);
            SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, query.length(), 0, false, false);
            Assert.assertEquals("DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END", element.getText());
        }
        {
            String query = "/* Issue */" + "DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END;";
            SQLParserContext context = createParserContext(hanaDialect, query);
            SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, query.length(), 0, false, false);
            Assert.assertEquals("DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END", element.getText());
        }
        {
            String query = "/* Issue */\n" + "DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END;";
            SQLParserContext context = createParserContext(setDialect("snowflake"), query);
            SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, query.length(), 0, false, false);
            Assert.assertEquals("/* Issue */\n" + "DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END", element.getText());
        }
        {
            String query = "/* Issue */\n\n" + "DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END;";
            SQLParserContext context = createParserContext(hanaDialect, query);
            SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, query.length(), 0, false, false);
            Assert.assertEquals("DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END", element.getText());
        }
        {
            String query = "DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END;";
            SQLParserContext context = createParserContext(hanaDialect, query);
            SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, query.length(), 0, false, false);
            Assert.assertEquals("DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END", element.getText());
        }
        {
            String query = "/* Issue */\n" + "DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END;";
            SQLDialect oracle = setDialect("oracle");
            Assert.assertFalse(oracle.isStripCommentsBeforeBlocks());
            SQLParserContext context = createParserContext(oracle, query);
            SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, query.length(), 0, false, false);
            Assert.assertEquals("/* Issue */\n" + "DO BEGIN\n" + "SELECT * FROM dummy;\n" + "END;", element.getText());
        }
    }


    @Test
    public void parseSnowflakeCreateProcedureWithIfStatements() throws DBException {
        String[] query = new String[]{
            "CREATE OR REPLACE PROCEDURE testproc()\n"
                + "RETURNS varchar\n"
                + "LANGUAGE SQL AS\n"
                + "$$\n"
                + "  DECLARE\n"
                + "    i int;\n"
                + "  BEGIN\n"
                + "    i:=1;\n"
                + "    IF (i=1) THEN\n"
                + "      i:=2;\n"
                + "    END IF;\n"
                + "    IF (i=2) THEN\n"
                + "      i:=3;\n"
                + "    END IF;\n"
                + "  END\n"
                + "$$"
        };
        assertParse("snowflake", query);
    }

    public void checkSmartBlankLineIsAStatementDelimiterMode() throws DBException {
        String[] query = new String[]{
            "DECLARE EXIT HANDLER FOR SQLEXCEPTION\n" +
                    "BEGIN\n" +
                    "GET DIAGNOSTICS CONDITION 1\n" +
                    "v_error_message = MESSAGE_TEXT, v_sqlstate = RETURNED_SQLSTATE, v_error_schema = SCHEMA_NAME, v_error_TABLE = TABLE_NAME, v_err_nbr = MYSQL_ERRNO;\n" +
                    "SELECT v_error_message, v_sqlstate, v_error_schema, v_error_table, v_err_nbr;\n" +
                    "SET v_updt_row_count = 1;\n" +
                    "END;\n",
            null,
            "use Elizabeth;\n",
            null,
            "SELECT Column1\n" +
            "FROM Elizabeth.Elis;\n",
            null,
            "use DBeaver;\n",
            null,
            "SELECT Column1, name\n" +
            "FROM DBeaver.NewTable;\n",
            null,
            "use Yan;\n",
            null,
            "SELECT AlbumId, Title, ArtistId, Column1\n" +
            "FROM Yan.Album;\n",
            null,
            "CREATE TABLE T_PARK_REQUIREMENT_APPLICATION(\n" +
                    "ID VARCHAR2(32) DEFAULT SYS_GUID() NOT NULL,\n" +
                    "COMPANY_NAME VARCHAR2(500),\n" +
                    "APPLICANT_NAME VARCHAR2(200),\n" +
                    "CONTACT_PHONE VARCHAR2(100),\n" +
                    "DATE_RAISED DATE,\n" +
                    "DEMAND_TYPE VARCHAR2(50),\n" +
                    "REPORT_TYPE VARCHAR2(50),\n" +
                    "REPORT_CONTENT VARCHAR2(4000),\n" +
                    "STATUS INT,\n" +
                    "CREATE_BY VARCHAR2(32),\n" +
                    "CREATE_TIME DATE,\n" +
                    "UPDATE_BY VARCHAR2(32),\n" +
                    "UPDATE_TIME DATE,\n" +
                    "PRIMARY KEY (ID)\n" +
                    ");\n",
            null,
            "COMMENT ON TABLE T_PARK_REQUIREMENT_APPLICATION IS '园区需求申请';\n",
            null,
            "COMMENT ON COLUMN T_PARK_REQUIREMENT_APPLICATION.ID IS '主键';\n",
            null,
            "COMMENT ON COLUMN T_PARK_REQUIREMENT_APPLICATION.COMPANY_NAME IS '公司名称';\n",
            null,
            "COMMENT ON COLUMN T_PARK_REQUIREMENT_APPLICATION.APPLICANT_NAME IS '申请人';\n",
            null,
            "select 1;\n",
            null,
            "select 2;\n",
            "select 10 ; -- Comments\n",
            null,
            "select 10 ; /* Comments */",
            null
        };
        assertParse("snowflake", query);
    }

    @Test
    public void parseSnowflakeIfExistsStatements() throws DBException {
        String[] query = new String[]{
            "DROP TABLE\r\n"
                + "IF\n"
                + "EXISTS dim_appt;",
            null,
            "DROP TABLE\n"
                + "IF EXISTS dim_test;",
            null,
            "CREATE or replace PROCEDURE example_if(flag INTEGER)\n" +
                "RETURNS VARCHAR\n" +
                "LANGUAGE SQL\n" +
                "AS\n" +
                "BEGIN\n" +
                "    IF (FLAG = 1) THEN\n" +
                "        RETURN 'one';\n" +
                "    ELSEIF (FLAG = 2) THEN\n" +
                "        RETURN 'two';\n" +
                "    ELSE\n" +
                "        RETURN 'Unexpected input.';\n" +
                "    END IF;\n" +
                "END;",
            null,
            "create or replace procedure test (customer_number integer)\n" +
                "    returns integer not null\n" +
                "    language sql\n" +
                "    as \n" +
                "$$\n" +
                "declare \n" +
                "seq integer;\n" +
                "\n" +
                "begin seq := 0;\n" +
                "if (customer_number = 1) then seq := 1;\n" +
                "    else if (customer_number = 0) then seq := seq + 10;\n" +
                "    end if;\n" +
                "end if;\n" +
                "    return seq;\n" +
                "end;\n" +
                "$$;",
            null,
            "CREATE TABLE IF NOT EXISTS MART_FLSEDW_CI.DEPLOYMENT_SCRIPTS\n"
                + "(\r\n"
                + "    DEPLOYMENT_SCRIPTS_ID INTEGER IDENTITY(1,1) NOT NULL\n"
                + "    , MODEL VARCHAR NOT NULL\n"
                + "    , TYPE VARCHAR NOT NULL\n"
                + "    , EXECUTION_DATE TIMESTAMP_LTZ NOT NULL DEFAULT CURRENT_TIMESTAMP\n"
                + "    , SCRIPT VARCHAR NOT NULL\n"
                + "    , HASHDIFF BINARY(16)\n"
                + ");",
            null,
            "ALTER PROCEDURE IF EXISTS procedure1(FLOAT) RENAME TO procedure2;",
            null
        };
        assertParse("snowflake", query);
    }

    @Test
    public void parseNamedParameters() throws DBException {
        List<String> inputParamNames = List.of("1", "\"SYs_B_1\"", "\"MyVar8\"", "AbC", "\"#d2\"");
        List<String> invalidParamNames = List.of("&6^34", "%#2", "\"\"\"\"");
        StringJoiner joiner = new StringJoiner(", ", "select ", " from dual");
        inputParamNames.stream().forEach(p -> joiner.add(":" + p));
        invalidParamNames.stream().forEach(p -> joiner.add(":" + p));
        String query = joiner.toString();
        SQLParserContext context = createParserContext(setDialect("snowflake"), query);
        List<SQLQueryParameter> params = SQLScriptParser.parseParametersAndVariables(context, 0, query.length());
        List<String> actualParamNames = new ArrayList<String>();
        for (SQLQueryParameter sqlQueryParameter : params) {
            actualParamNames.add(sqlQueryParameter.getName());
        }
        Assert.assertEquals(List.of("1", "\"SYs_B_1\"", "\"MyVar8\"", "AbC", "\"#d2\""), actualParamNames);
    }

    @Test
    public void parseVariables() throws DBException {
        List<String> inputParamNames = List.of("aBc", "PrE#%&@T", "a@c=");
        StringJoiner joiner = new StringJoiner(", ", "select ", " from dual");
        inputParamNames.stream().forEach(p -> joiner.add("${" + p + "}"));
        String query = joiner.toString();
        SQLParserContext context = createParserContext(setDialect("snowflake"), query);
        List<SQLQueryParameter> params = SQLScriptParser.parseParametersAndVariables(context, 0, query.length());
        List<String> actualParamNames = new ArrayList<String>();
        for (SQLQueryParameter sqlQueryParameter : params) {
            actualParamNames.add(sqlQueryParameter.getName());
        }
        Assert.assertEquals(List.of("aBc", "PrE#%&@T", "a@c="), actualParamNames);
    }

    @Test
    public void parseVariablesInStrings() throws DBException {
        List<String> inputParamNames = List.of("aBc", "PrET", "ac");
        StringJoiner joiner = new StringJoiner(", ", "select ", " from dual");
        inputParamNames.stream().forEach(p -> joiner.add("'${" + p + "}'"));
        String query = joiner.toString();
        SQLParserContext context = createParserContext(setDialect("snowflake"), query);
        List<SQLQueryParameter> params = SQLScriptParser.parseParametersAndVariables(context, 0, query.length());
        List<String> actualParamNames = new ArrayList<String>();
        for (SQLQueryParameter sqlQueryParameter : params) {
            actualParamNames.add(sqlQueryParameter.getName());
        }
        Assert.assertEquals(List.of("aBc", "PrET", "ac"), actualParamNames);
    }

    @Test
    public void parseVariablesInComment() throws DBException {
        List<String> inputParamNames = List.of("aBc", "PrET", "ac");
        StringJoiner joiner = new StringJoiner(", ", "-- ", " ");
        inputParamNames.stream().forEach(p -> joiner.add("${" + p + "}"));
        String query = joiner.toString();
        SQLParserContext context = createParserContext(setDialect("snowflake"), query);
        List<SQLQueryParameter> params = SQLScriptParser.parseParametersAndVariables(context, 0, query.length());
        List<String> actualParamNames = new ArrayList<String>();
        for (SQLQueryParameter sqlQueryParameter : params) {
            actualParamNames.add(sqlQueryParameter.getName());
        }
        Assert.assertEquals(List.of("aBc", "PrET", "ac"), actualParamNames);
    }

    @Test
    public void parseParameterFromSetCommand() throws DBException {
        List<String> varNames = List.of("aBc", "\"aBc\"", "\"a@c=\"");
        ArrayList<String> expectedCommandsText = new ArrayList<>();
        StringBuilder script = new StringBuilder();
        for (int i = 0; i < varNames.size(); i++) {
            expectedCommandsText.add("@set " + varNames.get(i) + " = 1");
            script.append(expectedCommandsText.get(i)).append("\n");
        }
        SQLParserContext context = createParserContext(setDialect("snowflake"), script.toString());
        assert context.getDataSource() != null;
        List<SQLScriptElement> elements = SQLScriptParser.parseScript(context.getDataSource(), script.toString());
        List<SQLControlCommand> commands = new ArrayList<>();
        List<String> actualCommandsText = new ArrayList<>();
        for (SQLScriptElement sqlScriptElement : elements) {
            if (sqlScriptElement instanceof SQLControlCommand cmd) {
                commands.add(cmd);
                actualCommandsText.add(cmd.getText());
            }
        }
        Assert.assertEquals(expectedCommandsText, actualCommandsText);
        String text;
        int end;
        for (int i = 0; i < varNames.size(); i++) {
            text = commands.get(i).getParameter();
            end = ScriptParameterRule.tryConsumeParameterName(context.getDialect(), text, 0);
            Assert.assertEquals(varNames.get(i), text.substring(0, end).trim());
        }
    }


    @Test
    public void parseMultilineParametersFromSetCommand() throws DBException {
        String commandText = """
            @@set myVar=the first line
            and the second line
            works@@""";
        SQLParserContext context = createParserContext(setDialect("snowflake"), commandText);
        assert context.getDataSource() != null;
        List<SQLScriptElement> elements = SQLScriptParser.parseScript(context.getDataSource(), commandText);
        Assert.assertEquals(1, elements.size());
        SQLScriptElement sqlScriptElement = elements.get(0);
        Assert.assertTrue(sqlScriptElement instanceof SQLControlCommand);
        SQLControlCommand cmd = (SQLControlCommand) sqlScriptElement;
        Assert.assertEquals(commandText, cmd.getText());
        int end = ScriptParameterRule.tryConsumeParameterName(context.getDialect(), cmd.getParameter(), 0);
        Assert.assertEquals("myVar", cmd.getParameter().substring(0, end).trim());
    }


    private void assertParse(String dialectName, String[] expected) throws DBException {
        String source = Arrays.stream(expected).filter(e -> e != null).collect(Collectors.joining());
        List<String> expectedParts = new ArrayList<>(expected.length);
        for (int i = 0; i < expected.length; i++) {
            if (i + 1 < expected.length && expected[i + 1] == null) {
                expectedParts.add(expected[i].replaceAll("[\\;]+$", ""));
                i++;
            } else {
                expectedParts.add(expected[i]);
            }
        }
        assertParse(dialectName, source, expectedParts.toArray(new String[0]));
    }

    private void assertParse(String dialectName, String query, String[] expected) throws DBException {
        SQLParserContext context = createParserContext(setDialect(dialectName), query);
        int docLen = context.getDocument().getLength();
        List<SQLScriptElement> elements = SQLScriptParser.extractScriptQueries(context, 0, docLen, false, false, false);
        for (int index = 0; index < expected.length; index++) {
            Assert.assertEquals(expected[index], elements.get(index).getText());
        }
        Assert.assertEquals(expected.length, elements.size());
    }

    private SQLParserContext createParserContext(SQLDialect dialect, String query) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, dataSourceContainer.getPreferenceStore());
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);
        return new SQLParserContext(dataSource, syntaxManager, ruleManager, new Document(query));
    }

    private SQLDialect setDialect(String name) throws DBException {
        SQLDialectMetadataRegistry registry = DBWorkbench.getPlatform().getSQLDialectRegistry();
        SQLDialect dialect = registry.getDialect(name).createInstance();
        try {
            Mockito.when(databaseMetaData.getIdentifierQuoteString()).thenReturn("\"");
        } catch (SQLException e) {
            throw new DBException("Can't initialize identifier quote string for dialect " + name, e);
        }
        ((JDBCSQLDialect) dialect).initDriverSettings(session, dataSource, databaseMetaData);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);

        return dialect;
    }

}
