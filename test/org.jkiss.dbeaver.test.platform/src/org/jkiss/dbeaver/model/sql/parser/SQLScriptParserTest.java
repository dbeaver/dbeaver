/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.OSGIMockDataTest;
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
import org.jkiss.dbeaver.osgi.test.runner.OSGITestRunner;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RunWith(OSGITestRunner.class)
public class SQLScriptParserTest extends OSGIMockDataTest {
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
        assertParse("postgresql",
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
            new String[]{
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
            });
    }

    @Test
    public void parseOracleDeclareBlock() throws DBException {
        assertParse("oracle",
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
        new String[]{
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
            });
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
        assertParse("oracle", withStatements);
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
        assertParse("oracle", packageBodyStatements);
    }
    
    @Test
    public void parseCurrentControlCommandsCursorHead() throws DBException {
        String query = "@set col1 = '1'\n"
            + "@set col2 = '2'\n"
            + "@set col3 = '3'\n"
            + "@set col4 = '4'\n"
            + "@set col5 = '5'\n"
            + "\n"
            + "SELECT 'test1' FROM daul;\n"
            + "\n"
            + "SELECT 'test2' FROM dual;";
        SQLParserContext context = createParserContext(setDialect("oracle"), query);
        SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, query.length(), 64, false, false);
        Assert.assertEquals("@set col5 = '5'", element.getText());
    }
    
    @Test
    public void parseCurrentControlCommandsCursorTail() throws DBException {
        String query = "@set col1 = '1'\n"
            + "@set col2 = '2'\n"
            + "@set col3 = '3'\n"
            + "@set col4 = '4'\n"
            + "@set col5 = '5'\n"
            + "\n"
            + "SELECT 'test1' FROM daul;\n"
            + "\n"
            + "SELECT 'test2' FROM dual;";
        SQLParserContext context = createParserContext(setDialect("oracle"), query);
        SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, query.length(), 15, false, false);
        Assert.assertEquals("@set col1 = '1'", element.getText());
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
            SQLParserContext context = createParserContext(setDialect("oracle"), qstring);
            TPRuleBasedScanner scanner = context.getScanner();
            scanner.setRange(context.getDocument(), 0, qstring.length());
            Assert.assertEquals(SQLTokenType.T_STRING, scanner.nextToken().getData());
            Assert.assertEquals(qstring.length() - 1, scanner.getTokenLength());
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
            SQLParserContext context = createParserContext(setDialect("oracle"), badQstring);
            TPRuleBasedScanner scanner = context.getScanner();
            scanner.setRange(context.getDocument(), 0, badQstring.length());
            Assert.assertNotEquals(SQLTokenType.T_STRING, scanner.nextToken().getData());
            Assert.assertNotEquals(badQstring.length() - 1, scanner.getTokenLength());
        }
    }
    
    /**
     * Check that QStringRule doesn't interfere in this case
     * See #19319
     */
    @Test
    public void parseOracleNamedByQTable() throws DBException {
        String query = "select * from q;";
        SQLParserContext context = createParserContext(setDialect("oracle"), query);
        TPRuleBasedScanner scanner = context.getScanner();
        scanner.setRange(context.getDocument(), 14, query.length());
        Assert.assertEquals(SQLTokenType.T_OTHER, scanner.nextToken().getData());
        Assert.assertEquals(1, scanner.getTokenLength());;
        Assert.assertEquals(SQLTokenType.T_DELIMITER, scanner.nextToken().getData());
        Assert.assertEquals(1, scanner.getTokenLength());
    }
    
    
    @Test
    public void parseBeginTransaction() throws DBException {
        String[] dialects = new String[] {"postgresql", "sqlserver"};
        for (String dialect : dialects) {
            assertParse(dialect,
                "begin transaction;\nselect 1 from dual;",
                new String[]{"begin transaction", "select 1 from dual"}
            );
        }
    }
    
    @Test
    public void parseFromCursorPositionBeginTransaction() throws DBException {
        String[] dialects = new String[] {"postgresql", "sqlserver"};
        String query = "begin transaction;\nselect 1 from dual;";
        SQLScriptElement element;
        SQLParserContext context;
        for (String dialect : dialects) {
            context = createParserContext(setDialect(dialect), query);
            int[] positions = new int[]{4, 18};
            for (int pos : positions) {
                element = SQLScriptParser.parseQuery(context, 0, query.length(), pos, false, false);
                Assert.assertEquals("begin transaction", element.getText());
            }
        }
    }

    /**
     * Issue 34731
     */
    @Test
    public void parseFromCursorPositionSmartModeNoDelimiterAndSpaceInside() throws DBException {
        String[] dialects = new String[] {"postgresql"};
        String query = """
            select *\s
            from (values(random())
                   \s
                ) as s(v)
            where s.v is not null
            """;
        SQLScriptElement element;
        SQLParserContext context;
        for (String dialect : dialects) {
            context = createParserContext(setDialect(dialect), query);
            int[] positions = new int[]{23, 58, 81};
            for (int pos : positions) {
                element = SQLScriptParser.parseQuery(context, 0, query.length(), pos, false, false);
                Assert.assertEquals(query, element.getText());
            }
        }
    }

    /**
     * Issue 34815
     */
    @Test
    public void parseFromCursorPositionSmartModeShowStatement() throws DBException {
        String[] queries = {"""
            select *
            from film_actor
                        
            show search_path;
            """,
            """
            select *
            from film_actor;
            show search_path;
            """
        };
        SQLScriptElement element;
        SQLParserContext context;
        for (String query : queries) {
            context = createParserContext(setDialect("postgresql"), query);
            int[] positions = new int[]{3, 17, 33, 45, 51, 59};
            for (int pos : positions) {
                if (pos >= query.length()) {
                    continue;
                }
                element = SQLScriptParser.parseQuery(context, 0, query.length(), pos, false, false);
                if (pos < 24) {
                    Assert.assertEquals("""
                        select *
                        from film_actor""", element.getText());
                } else {
                    Assert.assertEquals("show search_path", element.getText());
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
            select 10 ; -- Comments
            select 10 ; /* Comments */
            """;
        SQLParserContext context = createParserContext(setDialect("postgresql"), query);
        SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, query.length(), 3, false, false);
        Assert.assertEquals("select 10 ", element.getText());
        element = SQLScriptParser.parseQuery(context, 0, query.length(), 29, false, false);
        Assert.assertEquals("-- Comments\nselect 10 ", element.getText());
    }

    /**
     * Issue 22489
     */
    @Test
    public void parseFromCursorPositionUpdateWithWhereAndBlankLineInBetween() throws DBException {
        String query = """
            UPDATE orders
            SET is_deleted = true
            
            WHERE id in (1,23)
            """;
        SQLScriptElement element;
        SQLParserContext context = createParserContext(setDialect("postgresql"), query);
        if (context.getSyntaxManager().getStatementDelimiterMode().useSmart) {
            int[] positions = new int[]{8, 21, 36, 58};
            for (int pos : positions) {
                element = SQLScriptParser.parseQuery(context, 0, query.length(), pos, false, false);
                Assert.assertEquals(query, element.getText());
            }
        } else {
            int[] positions = new int[]{8, 21};
            for (int pos : positions) {
                element = SQLScriptParser.parseQuery(context, 0, query.length(), pos, false, false);
                Assert.assertEquals("""
                    UPDATE orders
                    SET is_deleted = true""", element.getText());
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
        SQLParserContext context = createParserContext(setDialect("postgresql"), query);
        SQLScriptElement element = SQLScriptParser.parseQuery(context, 0, query.length(), 8, false, false);
        Assert.assertEquals("""
            SELECT *
            FROM foo
            WHERE 1=1""", element.getText());
    }


    private void assertParse(String dialectName, String[] expected) throws DBException {
        String source = Arrays.stream(expected).filter(Objects::nonNull).collect(Collectors.joining());
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
        List<SQLScriptElement> elements = SQLScriptParser.extractScriptQueries(context, 0, context.getDocument().getLength(), false, false, false);
        Assert.assertEquals(expected.length, elements.size());
        for (int index = 0; index < expected.length; index++) {
            Assert.assertEquals(expected[index], elements.get(index).getText());
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
        if (name.equals("oracle")) {
            Mockito.when(dataSource.isServerVersionAtLeast(12, 1)).thenReturn(true);
        }
        if (name.equals("sqlserver")) {
            Mockito.when(driver.getSampleURL()).thenReturn("jdbc:sqlserver://localhost;user=MyUserName;password=*****;");
        }
        SQLDialect dialect = registry.getDialect(name).createInstance();
        ((JDBCSQLDialect) dialect).initDriverSettings(session, dataSource, databaseMetaData);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        return dialect;
    }
}