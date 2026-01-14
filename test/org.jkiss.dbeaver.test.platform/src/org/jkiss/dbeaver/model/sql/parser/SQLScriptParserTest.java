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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadataRegistry;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.util.SQLEditorTestUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SQLScriptParserTest extends DBeaverUnitTest {

    public static final String POSTGRESQL_DIALECT_NAME = "postgresql";
    public static final String ORACLE_DIALECT_NAME = "oracle";
    public static final String SQLSERVER_DIALECT_NAME = "sqlserver";

    @Mock
    private JDBCDataSource dataSource;
    @Mock
    private DBPDataSourceContainer dataSourceContainer;
    @Mock
    private DBCExecutionContext executionContext;
    @Mock
    private JDBCSession session;
    @Mock
    private JDBCDatabaseMetaData databaseMetaData;
    @Mock
    private DBPDriver driver;

    @Before
    public void init() {
        DBPConnectionConfiguration connectionConfiguration = new DBPConnectionConfiguration();
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        Mockito.when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        Mockito.lenient().when(dataSourceContainer.getConnectionConfiguration()).thenReturn(connectionConfiguration);
        Mockito.when(dataSourceContainer.getActualConnectionConfiguration()).thenReturn(connectionConfiguration);
        Mockito.when(dataSourceContainer.getPreferenceStore()).thenReturn(preferenceStore);
        Mockito.lenient().when(executionContext.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSourceContainer.getDriver()).thenReturn(driver);
    }

    @Test
    public void parsePostgresDoubleDollar() throws DBException {
        assertParse(
            POSTGRESQL_DIALECT_NAME,
            "CREATE OR REPLACE FUNCTION fn_TestDelimiter()\n" +
            "RETURNS BOOLEAN AS\n" +
            "$$\n" +
            "BEGIN  \n" +
            "\tINSERT INTO tbl_Students VALUES (1,'Anvesh');\n" +
            "\tRETURN TRUE; \n" +
            "END;\n" +
            "$$\n" +
            "LANGUAGE plpgsql; \n\n" +
            "CREATE FUNCTION sales_tax(subtotal real) RETURNS real AS $$\n" +
            "BEGIN\n" +
            "    RETURN subtotal * 0.06;\n" +
            "END;\n" +
            "$$ LANGUAGE plpgsql;",
            new String[] {
                "CREATE OR REPLACE FUNCTION fn_TestDelimiter()\n" +
                "RETURNS BOOLEAN AS\n" +
                "$$\n" +
                "BEGIN  \n" +
                "\tINSERT INTO tbl_Students VALUES (1,'Anvesh');\n" +
                "\tRETURN TRUE; \n" +
                "END;\n" +
                "$$\n" +
                "LANGUAGE plpgsql",
                "CREATE FUNCTION sales_tax(subtotal real) RETURNS real AS $$\n" +
                "BEGIN\n" +
                "    RETURN subtotal * 0.06;\n" +
                "END;\n" +
                "$$ LANGUAGE plpgsql",
            }
        );
    }

    @Test
    public void parseOracleDeclareBlock() throws DBException {
        assertParse(
            ORACLE_DIALECT_NAME,
            "DECLARE\n" +
            "BEGIN\n" +
            "    NULL;\n" +
            "END;\n" +

            "DECLARE\n" +
            "    text VARCHAR(10);\n" +
            "    PROCEDURE greet(text IN VARCHAR2)\n" +
            "    IS\n" +
            "    BEGIN\n" +
            "        dbms_output.put_line(text);\n" +
            "    END;\n" +
            "BEGIN\n" +
            "    text := 'hello';\n" +
            "    greet(text);\n" +
            "END;\n" +

            "DECLARE\n" +
            "    text VARCHAR(10);\n" +
            "    PROCEDURE greet(text IN VARCHAR2)\n" +
            "    IS\n" +
            "    BEGIN\n" +
            "        dbms_output.put_line(text);\n" +
            "    END;\n" +
            "BEGIN\n" +
            "    DECLARE\n" +
            "    BEGIN\n" +
            "        text := 'hello';\n" +
            "        greet(text);\n" +
            "    END;\n" +
            "END;\n" +

            "DECLARE\n" +
            "    TYPE EmpRecTyp IS RECORD (\n" +
            "        emp_id     NUMBER(6),\n" +
            "        emp_sal    NUMBER(8,2)\n" +
            "    );\n" +
            "    PROCEDURE raise_salary (emp_info EmpRecTyp) IS\n" +
            "    BEGIN\n" +
            "        UPDATE employees SET salary = salary + salary * 0.10\n" +
            "        WHERE employee_id = emp_info.emp_id;\n" +
            "    END raise_salary;\n" +
            "BEGIN\n" +
            "    NULL;\n" +
            "END;\n" +

            "DECLARE\n" +
            "  TYPE rec1_t IS RECORD (field1 VARCHAR2(16), field2 NUMBER, field3 DATE);\n" +
            "  TYPE rec2_t IS RECORD (id INTEGER NOT NULL := -1, \n" +
            "  name VARCHAR2(64) NOT NULL := '[anonymous]');\n" +
            "  rec1 rec1_t;\n" +
            "  rec2 rec2_t;\n" +
            "  rec3 employees%ROWTYPE;\n" +
            "  TYPE rec4_t IS RECORD (first_name employees.first_name%TYPE, \n" +
            "                         last_name employees.last_name%TYPE, \n" +
            "                         rating NUMBER);\n" +
            "  rec4 rec4_t;\n" +
            "BEGIN\n" +
            "  rec1.field1 := 'Yesterday';\n" +
            "  rec1.field2 := 65;\n" +
            "  rec1.field3 := TRUNC(SYSDATE-1);\n" +
            "  DBMS_OUTPUT.PUT_LINE(rec2.name);\n" +
            "END;\n" +

            "DECLARE\n" +
            "    test_v NUMBER:=0;\n" +
            "    FUNCTION test_f(value_in_v IN number)\n" +
            "    RETURN\n" +
            "        varchar2\n" +
            "    IS\n" +
            "        value_char_out VARCHAR2(10);\n" +
            "    BEGIN\n" +
            "        SELECT to_char(value_in_v) INTO value_char_out FROM dual;\n" +
            "        RETURN value_char_out;\n" +
            "    END; \n" +
            "BEGIN\n" +
            "    dbms_output.put_line('Start');\n" +
            "    dbms_output.put_line(test_v||chr(9)||test_f(test_v));\n" +
            "    dbms_output.put_line('End');\n" +
            "END;\n" +

            "DECLARE\n" +
            "    i int;\n" +
            "BEGIN\n" +
            "    i := 0;\n" +
            "    IF i < 5 THEN\n" +
            "        i := i + 1;\n" +
            "        DBMS_OUTPUT.PUT_LINE ('This is: '||i);\n" +
            "    END IF;\n" +
            "END;\n" +

            "CREATE TRIGGER TRI_CODE_SYSTEM\n" +
            "BEFORE INSERT ON CODE_SYSTEM\n" +
            "REFERENCING NEW AS NEWROW FOR EACH ROW\n" +
            "BEGIN ATOMIC\n" +
            "IF TRUE THEN\n" +
            "SIGNAL SQLSTATE '45000';\n" +
            "END IF;\n" +
            "END;\n" +

            "CREATE OR REPLACE PACKAGE MIG2 AUTHID CURRENT_USER AS\n" +
            "    PROCEDURE LOG(SEVERITY VARCHAR2, MSG CLOB);\n" +
            "END;" +

            "CREATE OR REPLACE PACKAGE emp_mgmt AS \n" +
            "    FUNCTION hire (last_name VARCHAR2, job_id VARCHAR2, \n" +
            "        manager_id NUMBER, salary NUMBER, \n" +
            "        commission_pct NUMBER, department_id NUMBER) \n" +
            "        RETURN NUMBER; \n" +
            "    FUNCTION create_dept(department_id NUMBER, location_id NUMBER) \n" +
            "        RETURN NUMBER; \n" +
            "    PROCEDURE remove_emp(employee_id NUMBER) IS BEGIN NULL; END;\n" +
            "    PROCEDURE remove_dept(department_id NUMBER) IS BEGIN NULL; END;\n" +
            "    PROCEDURE increase_sal(employee_id NUMBER, salary_incr NUMBER) IS BEGIN NULL; END;\n" +
            "    PROCEDURE increase_comm(employee_id NUMBER, comm_incr NUMBER) IS BEGIN NULL; END;\n" +
            "    no_comm EXCEPTION; \n" +
            "    no_sal EXCEPTION; \n" +
            "END emp_mgmt;",
            new String[] {
                "DECLARE\n" +
                "BEGIN\n" +
                "    NULL;\n" +
                "END;",

                "DECLARE\n" +
                "    text VARCHAR(10);\n" +
                "    PROCEDURE greet(text IN VARCHAR2)\n" +
                "    IS\n" +
                "    BEGIN\n" +
                "        dbms_output.put_line(text);\n" +
                "    END;\n" +
                "BEGIN\n" +
                "    text := 'hello';\n" +
                "    greet(text);\n" +
                "END;",

                "DECLARE\n" +
                "    text VARCHAR(10);\n" +
                "    PROCEDURE greet(text IN VARCHAR2)\n" +
                "    IS\n" +
                "    BEGIN\n" +
                "        dbms_output.put_line(text);\n" +
                "    END;\n" +
                "BEGIN\n" +
                "    DECLARE\n" +
                "    BEGIN\n" +
                "        text := 'hello';\n" +
                "        greet(text);\n" +
                "    END;\n" +
                "END;",

                "DECLARE\n" +
                "    TYPE EmpRecTyp IS RECORD (\n" +
                "        emp_id     NUMBER(6),\n" +
                "        emp_sal    NUMBER(8,2)\n" +
                "    );\n" +
                "    PROCEDURE raise_salary (emp_info EmpRecTyp) IS\n" +
                "    BEGIN\n" +
                "        UPDATE employees SET salary = salary + salary * 0.10\n" +
                "        WHERE employee_id = emp_info.emp_id;\n" +
                "    END raise_salary;\n" +
                "BEGIN\n" +
                "    NULL;\n" +
                "END;",

                "DECLARE\n" +
                "  TYPE rec1_t IS RECORD (field1 VARCHAR2(16), field2 NUMBER, field3 DATE);\n" +
                "  TYPE rec2_t IS RECORD (id INTEGER NOT NULL := -1, \n" +
                "  name VARCHAR2(64) NOT NULL := '[anonymous]');\n" +
                "  rec1 rec1_t;\n" +
                "  rec2 rec2_t;\n" +
                "  rec3 employees%ROWTYPE;\n" +
                "  TYPE rec4_t IS RECORD (first_name employees.first_name%TYPE, \n" +
                "                         last_name employees.last_name%TYPE, \n" +
                "                         rating NUMBER);\n" +
                "  rec4 rec4_t;\n" +
                "BEGIN\n" +
                "  rec1.field1 := 'Yesterday';\n" +
                "  rec1.field2 := 65;\n" +
                "  rec1.field3 := TRUNC(SYSDATE-1);\n" +
                "  DBMS_OUTPUT.PUT_LINE(rec2.name);\n" +
                "END;",

                "DECLARE\n" +
                "    test_v NUMBER:=0;\n" +
                "    FUNCTION test_f(value_in_v IN number)\n" +
                "    RETURN\n" +
                "        varchar2\n" +
                "    IS\n" +
                "        value_char_out VARCHAR2(10);\n" +
                "    BEGIN\n" +
                "        SELECT to_char(value_in_v) INTO value_char_out FROM dual;\n" +
                "        RETURN value_char_out;\n" +
                "    END; \n" +
                "BEGIN\n" +
                "    dbms_output.put_line('Start');\n" +
                "    dbms_output.put_line(test_v||chr(9)||test_f(test_v));\n" +
                "    dbms_output.put_line('End');\n" +
                "END;",

                "DECLARE\n" +
                "    i int;\n" +
                "BEGIN\n" +
                "    i := 0;\n" +
                "    IF i < 5 THEN\n" +
                "        i := i + 1;\n" +
                "        DBMS_OUTPUT.PUT_LINE ('This is: '||i);\n" +
                "    END IF;\n" +
                "END;",

                "CREATE TRIGGER TRI_CODE_SYSTEM\n" +
                "BEFORE INSERT ON CODE_SYSTEM\n" +
                "REFERENCING NEW AS NEWROW FOR EACH ROW\n" +
                "BEGIN ATOMIC\n" +
                "IF TRUE THEN\n" +
                "SIGNAL SQLSTATE '45000';\n" +
                "END IF;\n" +
                "END;",

                "CREATE OR REPLACE PACKAGE MIG2 AUTHID CURRENT_USER AS\n" +
                "    PROCEDURE LOG(SEVERITY VARCHAR2, MSG CLOB);\n" +
                "END;",

                "CREATE OR REPLACE PACKAGE emp_mgmt AS \n" +
                "    FUNCTION hire (last_name VARCHAR2, job_id VARCHAR2, \n" +
                "        manager_id NUMBER, salary NUMBER, \n" +
                "        commission_pct NUMBER, department_id NUMBER) \n" +
                "        RETURN NUMBER; \n" +
                "    FUNCTION create_dept(department_id NUMBER, location_id NUMBER) \n" +
                "        RETURN NUMBER; \n" +
                "    PROCEDURE remove_emp(employee_id NUMBER) IS BEGIN NULL; END;\n" +
                "    PROCEDURE remove_dept(department_id NUMBER) IS BEGIN NULL; END;\n" +
                "    PROCEDURE increase_sal(employee_id NUMBER, salary_incr NUMBER) IS BEGIN NULL; END;\n" +
                "    PROCEDURE increase_comm(employee_id NUMBER, comm_incr NUMBER) IS BEGIN NULL; END;\n" +
                "    no_comm EXCEPTION; \n" +
                "    no_sal EXCEPTION; \n" +
                "END emp_mgmt;"
            }
        );
    }

    @Test
    public void parseOracleWithBlock() throws DBException {
        String[] withStatements = new String[] {
            "WITH dept_count AS (\n" +
            "  SELECT deptno, COUNT(*) AS dept_count\n" +
            "    FROM emp\n" +
            "   GROUP BY deptno)\n" +
            "SELECT e.ename AS employee_name,\n" +
            "       dc.dept_count AS emp_dept_count\n" +
            "  FROM emp e\n" +
            "  JOIN dept_count dc ON e.deptno = dc.deptno;",
            null,
            "WITH\n" +
            "  dept_costs AS (\n" +
            "    SELECT dname, SUM(sal) dept_total\n" +
            "      FROM   emp e, dept d\n" +
            "     WHERE  e.deptno = d.deptno\n" +
            "     GROUP BY dname\n" +
            "\t ),\n" +
            "  avg_cost AS (\n" +
            "    SELECT SUM(dept_total)/COUNT(*) avg\n" +
            "      FROM dept_costs\n" +
            "\t  )\n" +
            "SELECT *\n" +
            "  FROM dept_costs\n" +
            " WHERE  dept_total > (SELECT avg FROM avg_cost)\n" +
            " ORDER BY dname;",
            null,
            "WITH\n" +
            "  FUNCTION with_function(p_id IN NUMBER) RETURN NUMBER IS\n" +
            "  BEGIN\n" +
            "    RETURN p_id;\n" +
            "  END;\n" +
            "  FUNCTION with_function2(p_id IN NUMBER) RETURN NUMBER IS\n" +
            "  BEGIN\n" +
            "    RETURN p_id;\n" +
            "  END;\n" +
            "SELECT with_function(id)\n" +
            "  FROM   t1\n" +
            " WHERE  rownum = 1;",
            null,
            "WITH\n" +
            "  PROCEDURE with_procedure(p_id IN NUMBER) IS\n" +
            "  BEGIN\n" +
            "    DBMS_OUTPUT.put_line('p_id=' || p_id);\n" +
            "  END;\n" +
            "SELECT id\n" +
            "  FROM   t1\n" +
            " WHERE  rownum = 1;",
            null,
        };
        assertParse(ORACLE_DIALECT_NAME, withStatements);
    }

    @Test
    public void parseOraclePackageBodyBlock() throws DBException {
        String[] packageBodyStatements = new String[] {
            "CREATE OR REPLACE NONEDITIONABLE PACKAGE BODY order_mgmt\n" +
            "AS\n" +
            "  -- get net value of a order\n" +
            "  FUNCTION get_net_value(\n" +
            "      p_order_id NUMBER)\n" +
            "    RETURN NUMBER\n" +
            "  IS\n" +
            "    ln_net_value NUMBER \n" +
            "  BEGIN\n" +
            "    SELECT\n" +
            "      SUM(unit_price * quantity)\n" +
            "    INTO\n" +
            "      ln_net_value\n" +
            "    FROM\n" +
            "      order_items\n" +
            "    WHERE\n" +
            "      order_id = p_order_id;\n" +
            "\n" +
            "    RETURN p_order_id;\n" +
            "\n" +
            "  EXCEPTION\n" +
            "  WHEN no_data_found THEN\n" +
            "    DBMS_OUTPUT.PUT_LINE( SQLERRM );\n" +
            "  END get_net_value;\n" +
            "\n" +
            "-- Get net value by customer\n" +
            "  FUNCTION get_net_value_by_customer(\n" +
            "      p_customer_id NUMBER,\n" +
            "      p_year        NUMBER)\n" +
            "    RETURN NUMBER\n" +
            "  IS\n" +
            "    ln_net_value NUMBER \n" +
            "  BEGIN\n" +
            "    SELECT\n" +
            "      SUM(quantity * unit_price)\n" +
            "    INTO\n" +
            "      ln_net_value\n" +
            "    FROM\n" +
            "      order_items\n" +
            "    INNER JOIN orders USING (order_id)\n" +
            "    WHERE\n" +
            "      extract(YEAR FROM order_date) = p_year\n" +
            "    AND customer_id                 = p_customer_id\n" +
            "    AND status                      = gc_shipped_status;\n" +
            "    RETURN ln_net_value;\n" +
            "  EXCEPTION\n" +
            "  WHEN no_data_found THEN\n" +
            "    DBMS_OUTPUT.PUT_LINE( SQLERRM );\n" +
            "  END get_net_value_by_customer;\n" +
            "\n" +
            "END order_mgmt;",

            "CREATE OR REPLACE EDITIONABLE PACKAGE BODY synchronize_my_data \n" +
            "IS\n" +
            "  PROCEDURE synchronize_data(p_run_date IN date) IS\n" +
            "      PROCEDURE process_deletes(p_run_date IN date) IS\n" +
            "      BEGIN\n" +
            "          dbms_output.put_line('Run Date: ' || to_char(p_run_date, 'MM/DD/YYYY'));      \n" +
            "      END;\n" +
            "  BEGIN\n" +
            "    process_deletes(p_run_date);\n" +
            "  END;\n" +
            "\n" +
            "END;",

            "CREATE OR REPLACE PACKAGE BODY synchronize_my_data \n" +
            "IS\n" +
            "  PROCEDURE process_deletes(p_run_date IN date) \n" +
            "  IS\n" +
            "  BEGIN\n" +
            "      dbms_output.put_line('Run Date: ' || to_char(p_run_date, 'MM/DD/YYYY'));      \n" +
            "  END process_deletes;\n" +
            "\n" +
            "  PROCEDURE synchronize_data(p_run_date IN date) \n" +
            "  IS\n" +
            "  BEGIN\n" +
            "    process_deletes(p_run_date);\n" +
            "  END synchronize_data;\n" +
            "\n" +
            "END synchronize_my_data;"

        };
        assertParse(ORACLE_DIALECT_NAME, packageBodyStatements);
    }

    /**
     * Issue 37925
     */
    @Test
    public void parseOracleProcedureFunctionAs() throws DBException {
        String[] statements = new String[] {
            "CREATE OR REPLACE FUNCTION TEST_1.FUNC_1 \n" +
            "RETURN NUMBER IS \n" +
            "n NUMBER := 0;\n" +
            "BEGIN\n" +
            "SELECT 1 INTO n from dual;\n" +
            "RETURN n;\n" +
            "END;",

            "CREATE OR REPLACE PROCEDURE my_procedure (\n" +
            "    p_id IN NUMBER\n" +
            ")\n" +
            "AS\n" +
            "    v_name VARCHAR2(100) DEFAULT 'Unknown';\n" +
            "BEGIN\n" +
            "    SELECT name INTO v_name\n" +
            "    FROM employees\n" +
            "    WHERE  employee_id = p_id;\n" +
            "\n" +
            "    DBMS_OUTPUT.PUT_LINE('Employee Name: ' || v_name);\n" +
            "END;"
        };
        assertParse(ORACLE_DIALECT_NAME, statements);
    }

    @Test
    public void parseCurrentControlCommandsCursorHead() throws DBException {
        String query = """
            @set col1 = '1'
            @set col2 = '2'
            @set col3 = '3'
            @set col4 = '4'
            @set col5 = '5'<-|
            
            SELECT 'test1' FROM dual;
            
            SELECT 'test2' FROM dual;
            """;
        var modifiedQueryAndPositions = SQLEditorTestUtil.getCursorPositions(query);
        String modifiedQuery = modifiedQueryAndPositions.keySet().iterator().next();
        int[] positions = modifiedQueryAndPositions.get(modifiedQuery);

        SQLParserContext context = createParserContext(setDialect(ORACLE_DIALECT_NAME), modifiedQuery);
        SQLScriptElement element = SQLScriptParser.parseQuery(
            context, 0, modifiedQuery.length(), positions[0], false, false
        );
        assertEquals("@set col5 = '5'", element.getText());
    }

    @Test
    public void parseCurrentControlCommandsCursorTail() throws DBException {
        String query = """
            @set col1 = '1'<-|
            @set col2 = '2'
            @set col3 = '3'
            @set col4 = '4'
            @set col5 = '5'
            
            SELECT 'test1' FROM dual;
            
            SELECT 'test2' FROM dual;
            """;
        var modifiedQueryAndPositions = SQLEditorTestUtil.getCursorPositions(query);
        String modifiedQuery = modifiedQueryAndPositions.keySet().iterator().next();
        int[] positions = modifiedQueryAndPositions.get(modifiedQuery);
        SQLParserContext context = createParserContext(setDialect(ORACLE_DIALECT_NAME), modifiedQuery);
        SQLScriptElement element = SQLScriptParser.parseQuery(
            context, 0, modifiedQuery.length(), positions[0], false, false
        );
        assertEquals("@set col1 = '1'", element.getText());
    }

    @Test
    public void parseMultilineCommandFromCursorPosition() throws DBException {
        String query = """
            <-|@@set<-| var1<-| = 'I have a<-| long text for
            mul<-|tiple<-| lines'@@
            
            SELEC<-|T var1 FROM dual;""";
        String expected = """
            @@set var1 = 'I have a long text for
            multiple lines'@@""";
        var modifiedQueryAndPositions = SQLEditorTestUtil.getCursorPositions(query);
        String modifiedQuery = modifiedQueryAndPositions.keySet().iterator().next();
        int[] positions = modifiedQueryAndPositions.get(modifiedQuery);
        SQLParserContext context = createParserContext(setDialect(ORACLE_DIALECT_NAME), modifiedQuery);
        for (var pos : positions) {
            SQLScriptElement element = SQLScriptParser.parseQuery(
                context, 0, modifiedQuery.length(), pos, false, false
            );
            assertEquals(expected, element.getText());
        }
    }

    @Test
    public void parseOracleQStringRule() throws DBException {
        final List<String> qstrings = List.of(
            "q'[What's a quote among friends?]';",
            "q'!What's a quote among friends?!';",
            "q'(That's a really funny 'joke'.)';",
            "q'#That's a really funny 'joke'.#';",
            "q''All the king's horses'';",
            "q'>All the king's horses>';",
            "q'['Hello,' said the child, who didn't like goodbyes.]';",
            "q'{'Hello,' said the child, who didn't like goodbyes.}';",
            "Q'('Hello,' said the child, who didn't like goodbyes.)';",
            "q'<'Hello,' said the child, who didn't like goodbyes.>';"
        );

        for (String qstring : qstrings) {
            SQLParserContext context = createParserContext(setDialect(ORACLE_DIALECT_NAME), qstring);
            TPRuleBasedScanner scanner = context.getScanner();
            scanner.setRange(context.getDocument(), 0, qstring.length());
            assertEquals(SQLTokenType.T_STRING, scanner.nextToken().getData());
            assertEquals(qstring.length() - 1, scanner.getTokenLength());
            scanner.nextToken();
        }
        final List<String> badQstrings = List.of(
            "q'(That''s a really funny ''joke''.(';",
            "q'#That's a really funny 'joke'.$';",
            "q'>All the king's horses<';",
            "q'<All the king's horses<';",
            "q'<All the king's horses<;",
            "q'<All the king's horses>;'",
            "q'abcd'"
        );

        for (String badQstring : badQstrings) {
            SQLParserContext context = createParserContext(setDialect(ORACLE_DIALECT_NAME), badQstring);
            TPRuleBasedScanner scanner = context.getScanner();
            scanner.setRange(context.getDocument(), 0, badQstring.length());
            assertNotEquals(SQLTokenType.T_STRING, scanner.nextToken().getData());
            assertNotEquals(badQstring.length() - 1, scanner.getTokenLength());
        }
    }

    /**
     * Check that QStringRule doesn't interfere in this case
     * See #19319
     */
    @Test
    public void parseOracleNamedByQTable() throws DBException {
        String query = "select * from q;";
        SQLParserContext context = createParserContext(setDialect(ORACLE_DIALECT_NAME), query);
        TPRuleBasedScanner scanner = context.getScanner();
        scanner.setRange(context.getDocument(), 14, query.length());
        assertEquals(SQLTokenType.T_OTHER, scanner.nextToken().getData());
        assertEquals(1, scanner.getTokenLength());

        assertEquals(SQLTokenType.T_DELIMITER, scanner.nextToken().getData());
        assertEquals(1, scanner.getTokenLength());
    }


    @Test
    public void parseBeginTransaction() throws DBException {
        String[] dialects = new String[] {POSTGRESQL_DIALECT_NAME, SQLSERVER_DIALECT_NAME};
        for (String dialect : dialects) {
            assertParse(
                dialect,
                "begin transaction;\nselect 1 from dual;",
                new String[] {"begin transaction", "select 1 from dual"}
            );
        }
    }

    @Test
    public void parseFromCursorPositionBeginTransaction() throws DBException {
        String[] dialects = new String[] {POSTGRESQL_DIALECT_NAME, SQLSERVER_DIALECT_NAME};
        String query = """
            begi<-|n transaction;<-|
            select 1 from dual;
            """;
        SQLScriptElement element;
        SQLParserContext context;

        var modifiedQueryAndPositions = SQLEditorTestUtil.getCursorPositions(query);

        for (var entry : modifiedQueryAndPositions.entrySet()) {
            String modifiedQuery = entry.getKey();
            int[] positions = entry.getValue();

            for (String dialect : dialects) {
                context = createParserContext(setDialect(dialect), modifiedQuery);
                for (int pos : positions) {
                    element = SQLScriptParser.parseQuery(
                        context, 0, modifiedQuery.length(), pos, false, false
                    );
                    assertEquals("begin transaction", element.getText());
                }
            }
        }
    }

    /**
     * Issue 34731
     */
    @Test
    public void parseFromCursorPositionSmartModeNoDelimiterAndSpaceInside() throws DBException {
        String[] dialects = new String[] {POSTGRESQL_DIALECT_NAME};
        String query = """
            select *\s
            from (values(<-|random())
                   \s
                ) as s(v)
            wh<-|ere s.v is not null
            """;

        var modifiedQueryAndPositions = SQLEditorTestUtil.getCursorPositions(query);
        String modifiedQuery = modifiedQueryAndPositions.keySet().iterator().next();
        int[] positions = modifiedQueryAndPositions.get(modifiedQuery);

        SQLScriptElement element;
        SQLParserContext context;
        for (String dialect : dialects) {
            context = createParserContext(setDialect(dialect), modifiedQuery);
            for (int pos : positions) {
                element = SQLScriptParser.parseQuery(context, 0, modifiedQuery.length(), pos, false, false);
                assertEquals(modifiedQuery, element.getText());
            }
        }
    }


    /**
     * Issue 34815
     */
    @Test
    public void parseFromCursorPositionSmartModeShowStatement() throws DBException {
        String[] queriesWithMarkers = {
            """
                sel<-|ect *
                fro<-|m film_actor<-|

                sho<-|w search_path;<-|
                """,
            """
                sel<-|ect *
                fro<-|m film_actor;<-|
                sho<-|w search_path;<-|
                """
        };

        SQLScriptElement element;
        SQLParserContext context;

        for (String queryWithMarkers : queriesWithMarkers) {
            Map<String, int[]> modifiedQueryAndPositions = SQLEditorTestUtil.getCursorPositions(queryWithMarkers);
            String modifiedQuery = modifiedQueryAndPositions.keySet().iterator().next();
            int[] positions = modifiedQueryAndPositions.get(modifiedQuery);

            context = createParserContext(setDialect(POSTGRESQL_DIALECT_NAME), modifiedQuery);

            for (int pos : positions) {
                if (pos >= modifiedQuery.length()) {
                    continue;
                }
                element = SQLScriptParser.parseQuery(context, 0, modifiedQuery.length(), pos, false, false);

                if (pos < modifiedQuery.indexOf("show search_path")) {
                    assertEquals(
                        """
                            select *
                            from film_actor""", element.getText()
                    );
                } else {
                    assertEquals("show search_path", element.getText());
                }
            }
        }
    }

    /**
     * Issue 26416
     */
    @Test
    public void parseFromCursorPositionDelimitersAndMultilineComments() throws DBException {
        String query = """
            sel<-|ect 10 ; -- Comments
            selec<-|t 10 ; /* Comments */
            """;

        var modifiedQueryAndPositions = SQLEditorTestUtil.getCursorPositions(query);
        String modifiedQuery = modifiedQueryAndPositions.keySet().iterator().next();
        int[] positions = modifiedQueryAndPositions.get(modifiedQuery);

        SQLParserContext context = createParserContext(setDialect(POSTGRESQL_DIALECT_NAME), modifiedQuery);
        SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, modifiedQuery.length(), positions[0], false, false);
        assertEquals("select 10 ", element.getText());

        element = SQLScriptParser.parseQuery(context, 0, modifiedQuery.length(), positions[1], false, false);
        assertEquals("-- Comments\nselect 10 ", element.getText());
    }

    /**
     * Issue 22489
     */
    @Test
    public void parseFromCursorPositionUpdateWithWhereAndBlankLineInBetween() throws DBException {
        String query = """
            UPDATE o<-|rders
            SET is_<-|deleted = true
            
            WHERE id in (1,23)
            """;
        var modifiedQueryAndPositions = SQLEditorTestUtil.getCursorPositions(query);
        String modifiedQuery = modifiedQueryAndPositions.keySet().iterator().next();
        int[] positions = modifiedQueryAndPositions.get(modifiedQuery);
        SQLScriptElement element;
        SQLParserContext context = createParserContext(setDialect(POSTGRESQL_DIALECT_NAME), modifiedQuery);
        if (!context.getSyntaxManager().getStatementDelimiterMode().useSmart) {

            for (int pos : positions) {
                element = SQLScriptParser.parseQuery(
                    context, 0, modifiedQuery.length(), pos, false, false
                );
                assertEquals(
                    """
                        UPDATE orders
                        SET is_deleted = true""", element.getText()
                );
            }
        }
    }

    /**
     * Issue 26843
     */
    @Test
    public void parseFromCursorPositionSelectWithWhereAndDelimiterBeforeSecondCondition() throws DBException {
        String query = """
            SELECT *
            FROM foo
            WHERE 1=1;
            
            AND bar=1
            """;
        SQLParserContext context = createParserContext(setDialect(POSTGRESQL_DIALECT_NAME), query);
        SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, query.length(), 8, false, false);
        assertEquals(
            """
                SELECT *
                FROM foo
                WHERE 1=1""", element.getText()
        );
    }


    private void assertParse(String dialectName, String[] expected) throws DBException {
        String source = Arrays.stream(expected)
            .filter(Objects::nonNull)
            .collect(Collectors.joining());

        List<String> expectedParts = new ArrayList<>();
        for (int i = 0; i < expected.length; ) {
            String segment = expected[i];
            if (i + 1 < expected.length && expected[i + 1] == null) {
                expectedParts.add(segment.replaceAll(";+$", ""));
                i += 2;
            } else {
                expectedParts.add(segment);
                i++;
            }
        }

        assertParse(dialectName, source, expectedParts.toArray(String[]::new));
    }


    private void assertParse(String dialectName, String query, String[] expected) throws DBException {
        SQLParserContext context = createParserContext(setDialect(dialectName), query);
        List<SQLScriptElement> elements = SQLScriptParser.extractScriptQueries(
            context,
            0,
            context.getDocument().getLength(),
            false,
            false,
            false
        );
        assertEquals(expected.length, elements.size());
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], elements.get(index).getText());
        }
    }

    private SQLParserContext createParserContext(SQLDialect dialect, String query) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, dataSourceContainer.getPreferenceStore());
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);
        Document document = new Document(query);
        return new SQLParserContext(dataSource, syntaxManager, ruleManager, document);
    }

    private SQLDialect setDialect(String name) throws DBException {
        SQLDialectMetadataRegistry registry = DBWorkbench.getPlatform().getSQLDialectRegistry();
        if (name.equals(ORACLE_DIALECT_NAME)) {
            Mockito.when(dataSource.isServerVersionAtLeast(12, 1)).thenReturn(true);
        }
        if (name.equals(SQLSERVER_DIALECT_NAME)) {
            Mockito.when(driver.getSampleURL()).thenReturn("jdbc:sqlserver://localhost;user=MyUserName;password=*****;");
        }
        SQLDialect dialect = registry.getDialect(name).createInstance();
        ((JDBCSQLDialect) dialect).initDriverSettings(session, dataSource, databaseMetaData);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        return dialect;
    }


}