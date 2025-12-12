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
package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.ext.postgresql.model.data.PostgreArrayValueHandler;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.formatters.NumberDataFormatter;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCNumberValueHandler;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.formatter.DataFormatterProfile;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.Types;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PostgreValueParserTest extends DBeaverUnitTest {

    private final PostgreDialect sqlDialect = new PostgreDialect();

    @Mock
    private PostgreDataSource dataSource;
    @Mock
    private DBCSession session;

    @Mock
    private PostgreDataType arrayDoubleItemType;
    @Mock
    private PostgreDataType arrayIntItemType;
    @Mock
    private PostgreDataType arrayStringItemType;
    @Mock
    private PostgreDataType arrayBooleanItemType;
    @Mock
    private PostgreDataType arrayStructItemType;
    @Mock
    private PostgreDataType stringItemType;
    @Mock
    private PostgreDataType intItemType;
    @Mock
    private PostgreDataType booleanItemType;
    @Mock
    private PostgreDataType doubleItemType;
    @Mock
    private PostgreDataType structItemType;

    private NumberDataFormatter numberDataFormatter = new NumberDataFormatter();
    @Mock
    private DataFormatterProfile dataFormatterProfile = new DataFormatterProfile("test_profile", new TestPreferenceStore());

    @Mock
    DBDFormatSettings dbdFormatSettings;

    @Before
    public void setUp() throws Exception {
        numberDataFormatter.init(doubleItemType, Locale.ENGLISH, new HashMap<>());
        setupGeneralWhenMocks();
    }

    @Test
    public void convertStringToValue() throws DBCException {
        Assert.assertEquals(1, PostgreValueParser.convertStringToValue(session, intItemType, "1"));
        Assert.assertEquals(1.111, PostgreValueParser.convertStringToValue(session, doubleItemType, "1.111"));
        Assert.assertEquals("A", PostgreValueParser.convertStringToValue(session, stringItemType, "A"));
        Assert.assertNotEquals("ABC", PostgreValueParser.convertStringToValue(session, stringItemType, "A"));
        Assert.assertNotEquals(123, PostgreValueParser.convertStringToValue(session, intItemType, "1"));
        Assert.assertArrayEquals(new String[]{"A", "B"},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayStringItemType, "{\"A\",\"B\"}"));
        Assert.assertArrayEquals(new Integer[]{1, 22},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayIntItemType, "{1,22}"));
        Assert.assertArrayEquals(new Double[]{1.1, 22.22},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{1.1,22.22}"));
        Assert.assertNotEquals(new Integer[]{33333, 22},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayIntItemType, "{1,22}"));


        JDBCCollection innerCollection1 = new JDBCCollection(session.getProgressMonitor(), doubleItemType,
                new JDBCNumberValueHandler(doubleItemType, dbdFormatSettings),
                new Double[]{1.1, 22.22});
        JDBCCollection innerCollection2 = new JDBCCollection(session.getProgressMonitor(), doubleItemType,
                new JDBCNumberValueHandler(doubleItemType, dbdFormatSettings),
                new Double[]{3.3, 44.44});
        Assert.assertArrayEquals(new Object[]{innerCollection1, innerCollection2},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{1.1,22.22},{3.3,44.44}}"));

        JDBCCollection innerCollection3 = new JDBCCollection(session.getProgressMonitor(), doubleItemType,
                new JDBCNumberValueHandler(doubleItemType, dbdFormatSettings),
                new Object[]{innerCollection1, innerCollection2});
        Assert.assertArrayEquals(new Object[]{ innerCollection3, innerCollection3 },
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{{1.1,22.22},{3.3,44.44}},{{1.1,22.22},{3.3,44.44}}}"));

        //Bad input data tests
        Assert.assertEquals("{{{1.1,22.22},{3.3,44.44}},{1.1,22.22},{3.3,44.44}}}",
            PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{{1.1,22.22},{3.3,44.44}},{1.1,22.22},{3.3,44.44}}}"));
        Assert.assertEquals("{{{1.1,22.22},3.3,44.44}},{1.1,22.22},{3.3,44.44}",
                PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{{1.1,22.22},3.3,44.44}},{1.1,22.22},{3.3,44.44}"));
        Assert.assertEquals("{{1.1,22.22},3.3,44.44}},{1.1,22.22},{3.3,",
                PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{1.1,22.22},3.3,44.44}},{1.1,22.22},{3.3,"));
        Assert.assertEquals("{{1.1,22.22},,44.44}},{1.1,22.22},{3.3,}",
                PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{1.1,22.22},,44.44}},{1.1,22.22},{3.3,}"));

        Boolean[] booleans = {true, false};
        Assert.assertEquals(true, PostgreValueParser.convertStringToValue(session, booleanItemType, "true"));
        Assert.assertNotEquals(false, PostgreValueParser.convertStringToValue(session, booleanItemType, "true"));
        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType, "{TRUE,FALSE}"));
        //todo: add support alternatives to "true/false"
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType, "{'t','f'}", true));
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType, "{'true','false'}", true));
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'1','0'}", true));
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'y','n'}", true));
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'yes,'no'}", true));
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'on,'off'}", true));
    }

    @Test
    public void parseSingleObject() throws DBCException {
        Assert.assertArrayEquals(new String[]{}, PostgreValueParser.parseSingleObject(""));
        Assert.assertArrayEquals(new String[]{"colA", " ColB"}, PostgreValueParser.parseSingleObject("colA, ColB"));
        Assert.assertArrayEquals(new String[]{"A", " B"}, PostgreValueParser.parseSingleObject("A, B"));
        Assert.assertArrayEquals(new String[]{"A", null, "B", null}, PostgreValueParser.parseSingleObject("A,,B,"));
        Assert.assertArrayEquals(new String[]{"A a", "", "B", ""}, PostgreValueParser.parseSingleObject("\"A a\",\"\",B,\"\""));
    }

    @Test
    public void generateObjectString() {
        Assert.assertEquals("(\"A\",\"B\")", PostgreValueParser.generateObjectString(new String[]{"A", "B"}));
        //todo: unquote numbers
        Assert.assertEquals("(\"1\",\"2\",\"3\")", PostgreValueParser.generateObjectString(new Integer[]{1, 2, 3}));
        Assert.assertEquals("(\"1.1\",\"2.22\",\"3.333\")", PostgreValueParser.generateObjectString(new Double[]{1.1, 2.22, 3.333}));
        Assert.assertEquals("(\"(1,2,3)\")", PostgreValueParser.generateObjectString(new Object[]{"(1,2,3)"}));
        Assert.assertEquals("(\"(1,2,3)\",\"(4,5,6)\")", PostgreValueParser.generateObjectString(new Object[]{"(1,2,3)","(4,5,6)"}));

        Assert.assertEquals("(\"{1.1,2.22,3.333}\",\"{1,2,3}\")",
                PostgreValueParser.generateObjectString(new Object[]{new Double[]{1.1, 2.22, 3.333}, new Integer[]{1, 2, 3}}));
        Assert.assertEquals("(\"{{1.1,2.22,3.333},{1.1,2.22,3.333}}\",\"qwerty\")",
                PostgreValueParser.generateObjectString(new Object[]{new Double[][]{{1.1, 2.22, 3.333},{1.1, 2.22, 3.333}},"qwerty"}));
    }

    @Test
    public void convertArrayToStringShouldEscapeQuotesCases() {
        final Function<String[], DBDCollection> make2d = values ->
            new JDBCCollection(session.getProgressMonitor(), stringItemType, PostgreArrayValueHandler.INSTANCE, values);

        final Function<String[][], DBDCollection> make3d = values ->
            new JDBCCollection(session.getProgressMonitor(), arrayStringItemType, PostgreArrayValueHandler.INSTANCE, Arrays.stream(values).map(make2d).toArray());

        final BiConsumer<String, DBDCollection> tester = (expected, collection) ->
            Assert.assertEquals(expected, PostgreArrayValueHandler.INSTANCE.getValueDisplayString(collection.getComponentType(), collection, DBDDisplayFormat.UI));

        tester.accept("NULL", make2d.apply(null));
        tester.accept("{}", make2d.apply(new String[]{}));
        tester.accept("{NULL,\"NULL\"}", make2d.apply(new String[]{null, "NULL"}));
        tester.accept("{one,two,\" four with spaces \",\"f{i,v}e\"}", make2d.apply(new String[]{"one", "two", " four with spaces ", "f{i,v}e"}));
        tester.accept("{{a,b,c},{d,e,f}}", make3d.apply(new String[][]{{"a", "b", "c"}, {"d", "e", "f"}}));
        tester.accept("{{\"\"},{\"{}\"},{\"\\\"\"}}", make3d.apply(new String[][]{{""}, {"{}"}, {"\""}}));
    }

    @Test
    public void convertArrayToStringShouldNotEscapeQuotesForNativeFormat() {
        final Function<String[], DBDCollection> make2d = values ->
            new JDBCCollection(session.getProgressMonitor(), stringItemType, PostgreArrayValueHandler.INSTANCE, values);

        final Function<String[][], DBDCollection> make3d = values ->
            new JDBCCollection(
                session.getProgressMonitor(),
                arrayStringItemType,
                PostgreArrayValueHandler.INSTANCE,
                Arrays.stream(values).map(make2d).toArray()
            );

        final BiConsumer<String, DBDCollection> tester = (expected, collection) ->
            Assert.assertEquals(
                expected,
                PostgreArrayValueHandler.INSTANCE.getValueDisplayString(collection.getComponentType(), collection, DBDDisplayFormat.NATIVE)
            );

        tester.accept("NULL", make2d.apply(null));
        tester.accept("{NULL,NULL}", make2d.apply(new String[] {null, "NULL"}));

        tester.accept("{}", make2d.apply(new String[] {}));
        tester.accept("{one,two, four with spaces ,f{i,v}e}",
            make2d.apply(new String[] {"one", "two", " four with spaces ", "f{i,v}e"})
        );

        tester.accept("{\"quote\"}", make2d.apply(new String[] {"\"quote\""}));

        tester.accept("{{a,b,c},{d,e,f}}", make3d.apply(new String[][] {{"a", "b", "c"}, {"d", "e", "f"}}));
        tester.accept("{{},{{}},{\"}}", make3d.apply(new String[][] {{""}, {"{}"}, {"\""}}));
    }

    @Test
    public void parseArrayString() throws DBCException {
        List<String> stringList = new ArrayList<>();
        stringList.add("A");
        stringList.add("B");
        Assert.assertEquals(stringList, PostgreValueParser.parseArrayString("{\"A\",\"B\"}", ","));
        Assert.assertNotEquals(stringList, PostgreValueParser.parseArrayString("{\"A\",\"B\",\"C\"}", ","));
        Assert.assertNotEquals(stringList, PostgreValueParser.parseArrayString("{\"A\",\"B\"}", "."));

        List<String> intList = new ArrayList<>();
        intList.add("1");
        intList.add("22");
        intList.add("333");
        Assert.assertEquals(intList, PostgreValueParser.parseArrayString("{1,22,333}", ","));
        //Assert.assertEquals(intList, PostgreValueParser.parseArrayString("ARRAY[1,22,333]", ","));// todo: add array format support

        List<String> doublesList = new ArrayList<>();
        doublesList.add("1.123");
        doublesList.add("2.1421324124421");
        Assert.assertEquals(doublesList, PostgreValueParser.parseArrayString("{1.123,2.1421324124421}", ","));
        Assert.assertNotEquals(doublesList, PostgreValueParser.parseArrayString("{1.123,2.1421324124421}", "."));
//        Assert.assertEquals(doublesList, PostgreValueParser.parseArrayString("ARRAY[1.123,2.1421324124421]", ","));

        //Infinity, -Infinity, NaN //todo

        List<String> intNullList = new ArrayList<>(intList);
        intNullList.add(null);
        Assert.assertEquals(intNullList, PostgreValueParser.parseArrayString("{1,22,333,NULL}", ","));

        List<List<String>> int2List = new ArrayList<>();
        int2List.add(intList);
        int2List.add(intList);
        Assert.assertEquals(int2List, PostgreValueParser.parseArrayString("{{1,22,333},{1,22,333}}", ","));
        Assert.assertEquals(int2List, PostgreValueParser.parseArrayString("[1:2]={{1,22,333},{1,22,333}}", ",")); // "[1:2]=" do nothing
//        Assert.assertEquals(int2List, PostgreValueParser.parseArrayString("ARRAY[[1,22,333],[1,22,333]]", ","));

        List<List<List<String>>> int3List = new ArrayList<>();
        int3List.add(int2List);
        int3List.add(int2List);
        Assert.assertEquals(int3List, PostgreValueParser.parseArrayString("{{{1,22,333},{1,22,333}},{{1,22,333},{1,22,333}}}", ","));

    }

    @Test
    public void testParsePrimitiveArray() {
        Assert.assertArrayEquals(
            new String[]{},
            PostgreValueParser.parsePrimitiveArray("{}", Function.identity(), String[]::new));
        Assert.assertArrayEquals(
            new String[]{null, "NULL"},
            PostgreValueParser.parsePrimitiveArray("{NULL,\"NULL\"}", Function.identity(), String[]::new));
        Assert.assertArrayEquals(
            new String[]{"ab", "cd", null, "NULL", " spa ce "},
            PostgreValueParser.parsePrimitiveArray("{ ab , cd ,NULL,\"NULL\",\" spa ce \"}", Function.identity(), String[]::new));
        Assert.assertArrayEquals(
            new Integer[]{1, null, 3},
            PostgreValueParser.parsePrimitiveArray("{1,NULL,3}", Integer::valueOf, Integer[]::new));

        Assert.assertThrows(
            "Array value must start with \"{\"",
            IllegalArgumentException.class,
            () -> PostgreValueParser.parsePrimitiveArray("1}", Function.identity(), String[]::new));
        Assert.assertThrows(
            "Unexpected \"}\" character",
            IllegalArgumentException.class,
            () -> PostgreValueParser.parsePrimitiveArray("{1,}", Function.identity(), String[]::new));
        Assert.assertThrows(
            "Unexpected \",\" character",
            IllegalArgumentException.class,
            () -> PostgreValueParser.parsePrimitiveArray("{,}", Function.identity(), String[]::new));
        Assert.assertThrows(
            "Unexpected end of input",
            IllegalArgumentException.class,
            () -> PostgreValueParser.parsePrimitiveArray("{1,", Function.identity(), String[]::new));
        Assert.assertThrows(
            "Junk after closing right brace",
            IllegalArgumentException.class,
            () -> PostgreValueParser.parsePrimitiveArray("{1},", Function.identity(), String[]::new));
    }

    private void setupGeneralWhenMocks() throws Exception {
//        Mockito.when(dataSource.getSQLDialect()).thenReturn(sqlDialect);
        Mockito.when(session.getProgressMonitor()).thenReturn(new VoidProgressMonitor());

//        Mockito.when(intItemType.getFullTypeName()).thenReturn("test_intItemType");
        Mockito.when(intItemType.getDataKind()).thenReturn(DBPDataKind.ANY);
        Mockito.when(intItemType.getTypeID()).thenReturn(Types.INTEGER);

//        Mockito.when(arrayIntItemType.getFullTypeName()).thenReturn("test_arrayIntItemType");
        Mockito.when(arrayIntItemType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
        Mockito.when(arrayIntItemType.getComponentType(session.getProgressMonitor())).thenReturn(intItemType);
        Mockito.when(arrayIntItemType.getArrayDelimiter()).thenReturn(",");

//        Mockito.when(doubleItemType.getFullTypeName()).thenReturn("test_doubleItemType");
        Mockito.when(doubleItemType.getDataKind()).thenReturn(DBPDataKind.NUMERIC);
        Mockito.when(doubleItemType.getTypeID()).thenReturn(Types.DOUBLE);

        Mockito.when(arrayDoubleItemType.getFullTypeName()).thenReturn("test_arrayDoubleItemType");
        Mockito.when(arrayDoubleItemType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
        Mockito.when(arrayDoubleItemType.getComponentType(session.getProgressMonitor())).thenReturn(doubleItemType);
        Mockito.when(arrayDoubleItemType.getArrayDelimiter()).thenReturn(",");

//        Mockito.when(stringItemType.getFullTypeName()).thenReturn("test_stringItemType");
        Mockito.when(stringItemType.getDataKind()).thenReturn(DBPDataKind.STRING);
        Mockito.when(stringItemType.getTypeID()).thenReturn(Types.VARCHAR);
        Mockito.when(stringItemType.getArrayDelimiter()).thenReturn(",");
//        Mockito.when(stringItemType.getDataSource()).thenReturn(dataSource);

//        Mockito.when(arrayStringItemType.getFullTypeName()).thenReturn("test_arrayStringItemType");
        Mockito.when(arrayStringItemType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
        Mockito.when(arrayStringItemType.getComponentType(session.getProgressMonitor())).thenReturn(stringItemType);
        Mockito.when(arrayStringItemType.getArrayDelimiter()).thenReturn(",");

//        Mockito.when(structItemType.getFullTypeName()).thenReturn("test_structItemType");
//        Mockito.when(structItemType.getDataKind()).thenReturn(DBPDataKind.STRUCT);
//        Mockito.when(structItemType.getTypeID()).thenReturn(Types.STRUCT);

//        Mockito.when(arrayStructItemType.getFullTypeName()).thenReturn("test_arrayStructItemType");
//        Mockito.when(arrayStructItemType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
//        Mockito.when(arrayStructItemType.getComponentType(session.getProgressMonitor())).thenReturn(structItemType);

//        Mockito.when(booleanItemType.getFullTypeName()).thenReturn("test_booleanItemType");
        Mockito.when(booleanItemType.getDataKind()).thenReturn(DBPDataKind.BOOLEAN);
        Mockito.when(booleanItemType.getTypeID()).thenReturn(Types.BOOLEAN);

//        Mockito.when(arrayBooleanItemType.getFullTypeName()).thenReturn("test_arrayBooleanItemType");
        Mockito.when(arrayBooleanItemType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
        Mockito.when(arrayBooleanItemType.getComponentType(session.getProgressMonitor())).thenReturn(booleanItemType);
        Mockito.when(arrayBooleanItemType.getArrayDelimiter()).thenReturn(",");

//        Mockito.when(dataFormatterProfile.createFormatter(DBDDataFormatter.TYPE_NAME_NUMBER, doubleItemType)).thenReturn(numberDataFormatter);

//        Mockito.when(dbdFormatSettings.getDataFormatterProfile()).thenReturn(dataFormatterProfile);
    }

}

