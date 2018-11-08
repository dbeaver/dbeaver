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
    public void shouldRemoveSpacesAroundCommentSymbol() {
/*
        //given
        String expectedString = "-- SELECT * FROM mytable;";
        String inputString = " -- SELECT * FROM mytable;";

        //when
        String formattedString = formatter.format(inputString, configuration);

        //then
        assertEquals(expectedString, formattedString);
*/
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
                .append("\tTABLE1 SET").append(lineBreak)
                .append("\t\ta = 2").append(lineBreak)
                .append("\tWHERE").append(lineBreak)
                .append("\t\ta = 1;").append(lineBreak).append(lineBreak)
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

