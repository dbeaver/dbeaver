/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.model.sql.parser;

import org.eclipse.jface.text.Document;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class SQLScriptParserTest {
    @Mock
    private DBPDataSource dataSource;
    @Mock
    private DBPDataSourceContainer dataSourceContainer;
    @Mock
    private DBCExecutionContext executionContext;

    @Before
    public void init() {
        DBPConnectionConfiguration connectionConfiguration = new DBPConnectionConfiguration();
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        Mockito.when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        Mockito.when(dataSourceContainer.getConnectionConfiguration()).thenReturn(connectionConfiguration);
        Mockito.when(dataSourceContainer.getActualConnectionConfiguration()).thenReturn(connectionConfiguration);
        Mockito.when(dataSourceContainer.getPreferenceStore()).thenReturn(preferenceStore);
        Mockito.when(executionContext.getDataSource()).thenReturn(dataSource);
    }

    @Test
    public void parsePostgresDoubleDollar() throws DBException {
        assertParse("postgresql",
            "do $a$\n\nbegin\n\traise notice 'hello';\nend\n\n$a$\n\n$a$\n\n$b$\n\n$b$\n\n$a$\n\n$a$$a$\n\ndo $$\ndeclare\nbegin\nnull;\nend $$\n\ndummy",
            new String[]{
                "do $a$\n\nbegin\n\traise notice 'hello';\nend\n\n$a$",
                "$a$\n\n$b$\n\n$b$\n\n$a$",
                "$a$$a$",
                "do $$\ndeclare\nbegin\nnull;\nend $$",
                "dummy"
            });
    }

    @Test
    public void parseOracleDeclareBlock() throws DBException {
        assertParse("oracle",
            "BEGIN\n" +
            "    BEGIN\n" +
            "    END;\n" +
            "END;\n" +

            "BEGIN\n" +
            "    NULL;\n" +
            "END;\n" +

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
                "BEGIN\n" +
                "    BEGIN\n" +
                "    END;\n" +
                "END;",

                "BEGIN\n" +
                "    NULL;\n" +
                "END;",

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
        return new SQLParserContext(() -> executionContext, syntaxManager, ruleManager, document);
    }

    private SQLDialect setDialect(String name) throws DBException {
        SQLDialectRegistry registry = SQLDialectRegistry.getInstance();
        SQLDialect dialect = registry.getDialect(name).createInstance();
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        return dialect;
    }
}