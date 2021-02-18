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
package org.jkiss.dbeaver.model.sql.format.tokenized;

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;


@RunWith(MockitoJUnitRunner.class)
public class SQLFormatterTokenizedTest {

    SQLFormatterTokenized formatter = new SQLFormatterTokenized();
    @Mock
    private SQLFormatterConfiguration configuration;
    @Mock
    private DBPPreferenceStore preferenceStore;
    @Mock
    private SQLSyntaxManager syntaxManager;

    private SQLDialect dialect = BasicSQLDialect.INSTANCE;
    private final String lineBreak = System.getProperty("line.separator");


    @Before
    public void init() throws Exception {
        Mockito.when(configuration.getSyntaxManager()).thenReturn(syntaxManager);
        String[] delimiters = new String[]{";"};
        Mockito.when(syntaxManager.getStatementDelimiters()).thenReturn(delimiters);
        Mockito.when(syntaxManager.getDialect()).thenReturn(dialect);
        Mockito.when(syntaxManager.getCatalogSeparator()).thenReturn(".");
        Mockito.when(configuration.getKeywordCase()).thenReturn(DBPIdentifierCase.UPPER);

        Mockito.when(configuration.getIndentString()).thenReturn("\t");
        Mockito.doReturn(preferenceStore).when(configuration).getPreferenceStore();

    }

    @Test
    public void shouldDoDefaultFormat() {
        //given
        String expectedString = getExpectedString();
        String inputString = "SELECT * FROM TABLE1 t WHERE a > 100 AND b BETWEEN 12 AND 45;  SELECT t.*, j1.x, j2.y FROM TABLE1 t JOIN JT1 j1 ON j1.a = t.a LEFT OUTER JOIN JT2 j2 ON j2.a = t.a AND j2.b = j1.b WHERE t.xxx NOT NULL;  DELETE FROM TABLE1 WHERE a = 1;  UPDATE TABLE1 SET a = 2 WHERE a = 1;  SELECT table1.id, table2.number, SUM(table1.amount) FROM table1 INNER JOIN table2 ON table.id = table2.table1_id WHERE table1.id IN ( SELECT table1_id FROM table3 WHERE table3.name = 'Foo Bar' AND table3.type = 'unknown_type') GROUP BY table1.id, table2.number ORDER BY table1.id;\n";

        Mockito.when(preferenceStore.getBoolean(Mockito.eq(ModelPreferences.SQL_FORMAT_LF_BEFORE_COMMA))).thenReturn(false);

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then

        assertEquals(expectedString, formattedString);
    }

    @Test
    public void shouldReturnEmptyStringWhenThereIsOnlyOneSpace() {
        //given
        String expectedString = "";
        String inputString = " ";

        //when
        String format = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, format);
    }

    @Test
    public void shouldMakeUpperCaseForKeywords() {
        //given
        String expectedString = "SELECT" + lineBreak + "\t*" + lineBreak + "FROM" + lineBreak + "\tmytable;";
        String inputString = "select * from mytable;";

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, formattedString);
    }

    @Test
    public void shouldRemoveSpacesBeforeCommentSymbol() {
        //given
        String expectedString = "SELECT" + lineBreak + "\t*" + lineBreak + "FROM" + lineBreak + "\ttable1;" + lineBreak + "-- SELECT * FROM mytable;";
        String inputString = "SELECT" + lineBreak + "\t*" + lineBreak + "FROM" + lineBreak + "\ttable1;" + lineBreak +" -- SELECT * FROM mytable;";

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, formattedString);
    }

    @Test
    public void shouldAddLineBreakIfScriptStartsWithComment() {
        //given
        String expectedString = lineBreak + "-- SELECT * FROM mytable;";
        String inputString = "-- SELECT * FROM mytable;";

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, formattedString);
    }

    @Test
    public void shouldAddLineBreakBeforeBraceBySpecialSetting() {
        //given
        String expectedString = getExpectedStringWithLineBreakBeforeBraces();
        String inputString = "SELECT (SELECT thecol FROM thetable) FROM dual";

        Mockito.when(preferenceStore.getBoolean(Mockito.eq(ModelPreferences.SQL_FORMAT_LF_BEFORE_COMMA))).thenReturn(false);
        Mockito.when(preferenceStore.getBoolean(Mockito.eq(ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET))).thenReturn(true);

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, formattedString);
    }


    @Test
    public void shouldAddIndentForName() {
        //given
        String expectedString = "SELECT"+lineBreak + "\tmy_field" + lineBreak + "FROM" + lineBreak + "\tmy_table";
        String inputString = "SELECT my_field FROM my_table";

        Mockito.when(preferenceStore.getBoolean(Mockito.eq(ModelPreferences.SQL_FORMAT_LF_BEFORE_COMMA))).thenReturn(false);
        Mockito.when(preferenceStore.getBoolean(Mockito.eq(ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET))).thenReturn(true);

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, formattedString);
    }

    @Test
    public void shouldDoDefaultFormatForCreateStatementWhenIndentSubstatementsInParenthesesOff() {
        //given
        String inputString = "CREATE TABLE Persons (PersonID int, LastName varchar(255), FirstName varchar(255), Address varchar(255), City varchar(255));";
        String expectedString = "CREATE TABLE Persons (PersonID int," + lineBreak + "LastName varchar(255)," + lineBreak + "FirstName varchar(255)," + lineBreak + "Address varchar(255)," + lineBreak + "City varchar(255));";

        Mockito.when(preferenceStore.getBoolean(Mockito.eq(ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET))).thenReturn(false);

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, formattedString);
    }

    @Test
    public void shouldDoDefaultFormatForCreateStatementWhenIndentSubstatementsInParenthesesOn() {
        //given
        String inputString = "CREATE TABLE Persons (PersonID int, LastName varchar(255), FirstName varchar(255), Address varchar(255), City varchar(255));";
        String expectedString = "CREATE TABLE Persons (" + lineBreak +
                "\tPersonID int," + lineBreak +
                "\tLastName varchar(255)," + lineBreak +
                "\tFirstName varchar(255)," + lineBreak +
                "\tAddress varchar(255)," + lineBreak +
                "\tCity varchar(255)" + lineBreak +
                ");";

        Mockito.when(preferenceStore.getBoolean(Mockito.eq(ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET))).thenReturn(true);

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, formattedString);
    }

    @Test
    public void shouldDoDefaultFormatForAlterStatementWhenIndentSubstatementsInParenthesesOn() {
        //given
        String inputString = "ALTER TABLE `users` ADD COLUMN (count_copy smallint(6) NOT NULL, status int(10) unsigned NOT NULL) AFTER `lastname`;";
        String expectedString = "ALTER TABLE `users` ADD COLUMN (" + lineBreak +
                "\tcount_copy SMALLINT(6) NOT NULL," + lineBreak +
                "\tstatus int(10) unsigned NOT NULL" + lineBreak +
                ") AFTER `lastname`;";

        Mockito.when(preferenceStore.getBoolean(Mockito.eq(ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET))).thenReturn(true);

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, formattedString);
    }

    @Test
    public void shouldDoDefaultFormatForValuesNestedInTheFunctionAndDoNotMakeALineBreakAfterTheCommaForThem() {
        //given
        String inputString = "SELECT to_date(CONCAT(YEAR('2019-12-31'),'-',lpad(CEIL(MONTH('2019-12-31')/3)*3-2, 2, 0),'-01')) AS season_first_day"; //#7509
        String expectedString = "SELECT" + lineBreak +
                "\tto_date(CONCAT(YEAR('2019-12-31'), '-', lpad(CEIL(MONTH('2019-12-31')/ 3)* 3-2, 2, 0), '-01')) AS season_first_day";

        Mockito.when(configuration.isFunction("to_date")).thenReturn(true);
        Mockito.when(configuration.isFunction("lpad")).thenReturn(true);

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, formattedString);
    }

    @Test
    public void shouldDoDefaultFormatForSubSelectAndForValuesNestedInTheFunctionAndDoNotMakeALineBreakAfterTheCommaForThem() {
        //given
        String inputString = "CREATE VIEW bi_gaz_check_curve AS (SELECT cal.date, pay.check_id, COALESCE(pay.base_amount, pay.amount) amount_ars, pay.future_pay_due_date due_date, pay.cleared_date, pay.check_date issued_date FROM (SELECT generate_series('2010-01-01'::date, '2050-12-31'::date, INTERVAL '1 day') date, 1 payment_id) cal LEFT JOIN oracle.ap_checks_all pay ON cal.date >= pay.check_date AND cal.date <= (pay.future_pay_due_date::date + 30));"; //#9365
        String expectedString = "CREATE VIEW bi_gaz_check_curve AS (" + lineBreak +
                "SELECT" + lineBreak +
                "\tcal.date," + lineBreak +
                "\tpay.check_id," + lineBreak +
                "\tCOALESCE(pay.base_amount, pay.amount) amount_ars," + lineBreak +
                "\tpay.future_pay_due_date due_date," + lineBreak +
                "\tpay.cleared_date," + lineBreak +
                "\tpay.check_date issued_date" + lineBreak +
                "FROM" + lineBreak +
                "\t(" + lineBreak +
                "\tSELECT" + lineBreak +
                "\t\tgenerate_series('2010-01-01'::date, '2050-12-31'::date, INTERVAL '1 day') date," + lineBreak +
                "\t\t1 payment_id) cal" + lineBreak +
                "LEFT JOIN oracle.ap_checks_all pay ON" + lineBreak +
                "\tcal.date >= pay.check_date" + lineBreak +
                "\tAND cal.date <= (pay.future_pay_due_date::date + 30));";

        Mockito.when(configuration.isFunction("COALESCE")).thenReturn(true);
        Mockito.when(configuration.isFunction("generate_series")).thenReturn(true);

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, formattedString);
    }


    private String getExpectedStringWithLineBreakBeforeBraces() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT").append(lineBreak)
                .append("\t(").append(lineBreak)
                .append("\t\tSELECT").append(lineBreak)
                .append("\t\t\tthecol").append(lineBreak)
                .append("\t\tFROM").append(lineBreak)
                .append("\t\t\tthetable").append(lineBreak)
                .append("\t)").append(lineBreak)
                .append("FROM").append(lineBreak).append("\tdual");
        return sb.toString();
    }


    private String getExpectedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT").append(lineBreak)
                .append("\t*").append(lineBreak)
                .append("FROM").append(lineBreak)
                .append("\tTABLE1 t").append(lineBreak)
                .append("WHERE").append(lineBreak)
                .append("\ta > 100").append(lineBreak)
                .append("\tAND b BETWEEN 12 AND 45;").append(lineBreak).append(lineBreak)
                .append("SELECT").append(lineBreak)
                .append("\tt.*,").append(lineBreak)
                .append("\tj1.x,").append(lineBreak)
                .append("\tj2.y").append(lineBreak)
                .append("FROM").append(lineBreak)
                .append("\tTABLE1 t").append(lineBreak)
                .append("JOIN JT1 j1 ON").append(lineBreak)
                .append("\tj1.a = t.a").append(lineBreak)
                .append("LEFT OUTER JOIN JT2 j2 ON").append(lineBreak)
                .append("\tj2.a = t.a").append(lineBreak)
                .append("\tAND j2.b = j1.b").append(lineBreak)
                .append("WHERE").append(lineBreak)
                .append("\tt.xxx NOT NULL;").append(lineBreak).append(lineBreak)
                .append("DELETE").append(lineBreak)
                .append("FROM").append(lineBreak)
                .append("\tTABLE1").append(lineBreak)
                .append("WHERE").append(lineBreak)
                .append("\ta = 1;").append(lineBreak).append(lineBreak)
                .append("UPDATE").append(lineBreak)
                .append("\tTABLE1").append(lineBreak)
                .append("SET").append(lineBreak)
                .append("\ta = 2").append(lineBreak)
                .append("WHERE").append(lineBreak)
                .append("\ta = 1;").append(lineBreak).append(lineBreak)
                .append("SELECT").append(lineBreak)
                .append("\ttable1.id,").append(lineBreak)
                .append("\ttable2.number,").append(lineBreak)
                .append("\tSUM(table1.amount)").append(lineBreak)
                .append("FROM").append(lineBreak)
                .append("\ttable1").append(lineBreak)
                .append("INNER JOIN table2 ON").append(lineBreak)
                .append("\ttable.id = table2.table1_id").append(lineBreak)
                .append("WHERE").append(lineBreak)
                .append("\ttable1.id IN (").append(lineBreak)
                .append("\tSELECT").append(lineBreak)
                .append("\t\ttable1_id").append(lineBreak)
                .append("\tFROM").append(lineBreak)
                .append("\t\ttable3").append(lineBreak)
                .append("\tWHERE").append(lineBreak)
                .append("\t\ttable3.name = 'Foo Bar'").append(lineBreak)
                .append("\t\tAND table3.type = 'unknown_type')").append(lineBreak)
                .append("GROUP BY").append(lineBreak)
                .append("\ttable1.id,").append(lineBreak)
                .append("\ttable2.number").append(lineBreak)
                .append("ORDER BY").append(lineBreak)
                .append("\ttable1.id;").append(lineBreak);
        return sb.toString();
    }


}

