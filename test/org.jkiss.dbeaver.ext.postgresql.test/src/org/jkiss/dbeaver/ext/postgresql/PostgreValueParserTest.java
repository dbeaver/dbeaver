/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataTypeAttribute;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.ext.postgresql.model.data.PostgreArrayValueHandler;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.formatters.NumberDataFormatter;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCComposite;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCNumberValueHandler;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.formatter.DataFormatterProfile;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Mock
    private PostgreDataType compositeArrayType;

    @Mock
    private PostgreDataType outerCompositeType;

    @Mock
    private PostgreDataType innerCompositeType;

    @Mock
    private PostgreDataTypeAttribute outerFirstAttribute;

    @Mock
    private PostgreDataTypeAttribute outerSecondAttribute;

    @Mock
    private PostgreDataTypeAttribute innerFirstAttribute;

    @Mock
    private PostgreDataTypeAttribute innerSecondAttribute;

    @Mock
    private DBPDataSourceContainer container;

    @Mock
    private DBPDriver driver;

    @Mock
    private DBPDataSourceProviderDescriptor providerDescriptor;

    @Mock
    private PostgreServerExtension serverType;

    private NumberDataFormatter numberDataFormatter = new NumberDataFormatter();
    @Mock
    private DataFormatterProfile dataFormatterProfile = new DataFormatterProfile("test_profile", new TestPreferenceStore());

    @Mock
    DBDFormatSettings dbdFormatSettings;

    @BeforeEach
    public void setUp() throws Exception {
        numberDataFormatter.init(doubleItemType, Locale.ENGLISH, new HashMap<>());
        setupGeneralWhenMocks();
    }

    @Test
    public void convertCompositeArrayStringPreservesDimensions() throws DBException {
        setupCompositeArrayMocks();

        // PostgreSQL text representation of a 3D array with a single recti element
        String value = "{{{" + createArrayElement(13, 12, 13, 14) + "}}}";
        Object[] outerArray = parseCompositeArray(value);

        JDBCCollection secondDimension = Assertions.assertInstanceOf(JDBCCollection.class, outerArray[0]);
        JDBCCollection thirdDimension = Assertions.assertInstanceOf(JDBCCollection.class, secondDimension.getItem(0));
        assertRecti(thirdDimension.getItem(0), 13, 12, 13, 14);

        Assertions.assertEquals(value, getArrayDisplayString(outerArray, DBDDisplayFormat.NATIVE));
        Assertions.assertEquals("{{{\"((13,12),(13,14))\"}}}", getArrayDisplayString(outerArray, DBDDisplayFormat.UI));
    }

    @Test
    public void editStringOfCompositeArrayCanBeParsedBack() throws DBException {
        setupCompositeArrayMocks();

        String nativeValue = "{{{" + createArrayElement(11, 12, 13, 14) + "}}}";
        Object[] array = parseCompositeArray(nativeValue);

        String editValue = getArrayDisplayString(array, DBDDisplayFormat.EDIT);
        Assertions.assertEquals(nativeValue, editValue);

        String editedValue = editValue.replace("(11,12)", "(15,12)");
        Object[] editedArray = parseCompositeArray(editedValue);
        JDBCCollection secondDimension = Assertions.assertInstanceOf(JDBCCollection.class, editedArray[0]);
        JDBCCollection thirdDimension = Assertions.assertInstanceOf(JDBCCollection.class, secondDimension.getItem(0));

        assertRecti(thirdDimension.getItem(0), 15, 12, 13, 14);
        Assertions.assertEquals(
            "{{{" + createArrayElement(15, 12, 13, 14) + "}}}",
            getArrayDisplayString(editedArray, DBDDisplayFormat.NATIVE));
    }

    @Test
    public void convertSingleDimensionCompositeArray() throws DBException {
        setupCompositeArrayMocks();

        String value = "{" + createArrayElement(11, 12, 13, 14) + "," + createArrayElement(21, 22, 23, 24) + "}";
        Object[] array = parseCompositeArray(value);

        Assertions.assertEquals(2, array.length);
        assertRecti(array[0], 11, 12, 13, 14);
        assertRecti(array[1], 21, 22, 23, 24);
        Assertions.assertEquals(value, getArrayDisplayString(array, DBDDisplayFormat.NATIVE));
    }

    @Test
    public void convertTwoDimensionalCompositeArrayPreservesAllElements() throws DBException {
        setupCompositeArrayMocks();

        String value = "{{"
            + createArrayElement(11, 12, 13, 14) + "," + createArrayElement(21, 22, 23, 24)
            + "},{"
            + createArrayElement(31, 32, 33, 34) + "," + createArrayElement(41, 42, 43, 44)
            + "}}";
        Object[] array = parseCompositeArray(value);

        Assertions.assertEquals(2, array.length);
        JDBCCollection firstRow = Assertions.assertInstanceOf(JDBCCollection.class, array[0]);
        JDBCCollection secondRow = Assertions.assertInstanceOf(JDBCCollection.class, array[1]);
        Assertions.assertEquals(2, firstRow.getItemCount());
        Assertions.assertEquals(2, secondRow.getItemCount());
        assertRecti(firstRow.getItem(0), 11, 12, 13, 14);
        assertRecti(firstRow.getItem(1), 21, 22, 23, 24);
        assertRecti(secondRow.getItem(0), 31, 32, 33, 34);
        assertRecti(secondRow.getItem(1), 41, 42, 43, 44);
        Assertions.assertEquals(value, getArrayDisplayString(array, DBDDisplayFormat.NATIVE));
    }

    @Test
    public void convertCompositeArrayWithNullElement() throws DBException {
        setupCompositeArrayMocks();

        String value = "{" + createArrayElement(11, 12, 13, 14) + ",NULL}";
        Object[] array = parseCompositeArray(value);

        Assertions.assertEquals(2, array.length);
        assertRecti(array[0], 11, 12, 13, 14);
        Assertions.assertTrue(Assertions.assertInstanceOf(JDBCComposite.class, array[1]).isNull());
        Assertions.assertEquals(value, getArrayDisplayString(array, DBDDisplayFormat.NATIVE));
    }

    @Test
    public void convertEmptyCompositeArray() throws DBException {
        setupCompositeArrayMocks();

        Object[] array = parseCompositeArray("{}");

        Assertions.assertEquals(0, array.length);
        Assertions.assertEquals("{}", getArrayDisplayString(array, DBDDisplayFormat.NATIVE));
    }

    @Test
    public void convertStringToValue() throws DBCException {
        Assertions.assertEquals(1, PostgreValueParser.convertStringToValue(session, intItemType, "1"));
        Assertions.assertEquals(1.111, PostgreValueParser.convertStringToValue(session, doubleItemType, "1.111"));
        Assertions.assertEquals("A", PostgreValueParser.convertStringToValue(session, stringItemType, "A"));
        Assertions.assertNotEquals("ABC", PostgreValueParser.convertStringToValue(session, stringItemType, "A"));
        Assertions.assertNotEquals(123, PostgreValueParser.convertStringToValue(session, intItemType, "1"));
        Assertions.assertArrayEquals(new String[]{"A", "B"},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayStringItemType, "{\"A\",\"B\"}"));
        Assertions.assertArrayEquals(new Integer[]{1, 22},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayIntItemType, "{1,22}"));
        Assertions.assertArrayEquals(new Double[]{1.1, 22.22},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{1.1,22.22}"));
        Assertions.assertNotEquals(new Integer[]{33333, 22},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayIntItemType, "{1,22}"));


        JDBCCollection innerCollection1 = new JDBCCollection(session.getProgressMonitor(), doubleItemType,
                new JDBCNumberValueHandler(doubleItemType, dbdFormatSettings),
                new Double[]{1.1, 22.22});
        JDBCCollection innerCollection2 = new JDBCCollection(session.getProgressMonitor(), doubleItemType,
                new JDBCNumberValueHandler(doubleItemType, dbdFormatSettings),
                new Double[]{3.3, 44.44});
        Assertions.assertArrayEquals(new Object[]{innerCollection1, innerCollection2},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{1.1,22.22},{3.3,44.44}}"));

        JDBCCollection innerCollection3 = new JDBCCollection(session.getProgressMonitor(), doubleItemType,
                new JDBCNumberValueHandler(doubleItemType, dbdFormatSettings),
                new Object[]{innerCollection1, innerCollection2});
        Assertions.assertArrayEquals(new Object[]{ innerCollection3, innerCollection3 },
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{{1.1,22.22},{3.3,44.44}},{{1.1,22.22},{3.3,44.44}}}"));

        //Bad input data tests
        Assertions.assertEquals("{{{1.1,22.22},{3.3,44.44}},{1.1,22.22},{3.3,44.44}}}",
            PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{{1.1,22.22},{3.3,44.44}},{1.1,22.22},{3.3,44.44}}}"));
        Assertions.assertEquals("{{{1.1,22.22},3.3,44.44}},{1.1,22.22},{3.3,44.44}",
                PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{{1.1,22.22},3.3,44.44}},{1.1,22.22},{3.3,44.44}"));
        Assertions.assertEquals("{{1.1,22.22},3.3,44.44}},{1.1,22.22},{3.3,",
                PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{1.1,22.22},3.3,44.44}},{1.1,22.22},{3.3,"));
        Assertions.assertEquals("{{1.1,22.22},,44.44}},{1.1,22.22},{3.3,}",
                PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{{1.1,22.22},,44.44}},{1.1,22.22},{3.3,}"));

        Boolean[] booleans = {true, false};
        Assertions.assertEquals(true, PostgreValueParser.convertStringToValue(session, booleanItemType, "true"));
        Assertions.assertNotEquals(false, PostgreValueParser.convertStringToValue(session, booleanItemType, "true"));
        Assertions.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType, "{TRUE,FALSE}"));
        //todo: add support alternatives to "true/false"
//        Assertions.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType, "{'t','f'}", true));
//        Assertions.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType, "{'true','false'}", true));
//        Assertions.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'1','0'}", true));
//        Assertions.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'y','n'}", true));
//        Assertions.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'yes,'no'}", true));
//        Assertions.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'on,'off'}", true));
    }

    @Test
    public void parseSingleObject() throws DBCException {
        Assertions.assertArrayEquals(new String[]{}, PostgreValueParser.parseSingleObject(""));
        Assertions.assertArrayEquals(new String[]{"colA", " ColB"}, PostgreValueParser.parseSingleObject("colA, ColB"));
        Assertions.assertArrayEquals(new String[]{"A", " B"}, PostgreValueParser.parseSingleObject("A, B"));
        Assertions.assertArrayEquals(new String[]{"A", null, "B", null}, PostgreValueParser.parseSingleObject("A,,B,"));
        Assertions.assertArrayEquals(new String[]{"A a", "", "B", ""}, PostgreValueParser.parseSingleObject("\"A a\",\"\",B,\"\""));
    }

    @Test
    public void generateObjectString() {
        Assertions.assertEquals("(\"A\",\"B\")", PostgreValueParser.generateObjectString(new String[]{"A", "B"}));
        //todo: unquote numbers
        Assertions.assertEquals("(\"1\",\"2\",\"3\")", PostgreValueParser.generateObjectString(new Integer[]{1, 2, 3}));
        Assertions.assertEquals("(\"1.1\",\"2.22\",\"3.333\")", PostgreValueParser.generateObjectString(new Double[]{1.1, 2.22, 3.333}));
        Assertions.assertEquals("(\"(1,2,3)\")", PostgreValueParser.generateObjectString(new Object[]{"(1,2,3)"}));
        Assertions.assertEquals("(\"(1,2,3)\",\"(4,5,6)\")", PostgreValueParser.generateObjectString(new Object[]{"(1,2,3)","(4,5,6)"}));

        Assertions.assertEquals("(\"{1.1,2.22,3.333}\",\"{1,2,3}\")",
                PostgreValueParser.generateObjectString(new Object[]{new Double[]{1.1, 2.22, 3.333}, new Integer[]{1, 2, 3}}));
        Assertions.assertEquals("(\"{{1.1,2.22,3.333},{1.1,2.22,3.333}}\",\"qwerty\")",
                PostgreValueParser.generateObjectString(new Object[]{new Double[][]{{1.1, 2.22, 3.333},{1.1, 2.22, 3.333}},"qwerty"}));
    }

    @Test
    public void convertArrayToStringShouldEscapeQuotesCases() {
        final Function<String[], DBDCollection> make2d = values ->
            new JDBCCollection(session.getProgressMonitor(), stringItemType, PostgreArrayValueHandler.INSTANCE, values);

        final Function<String[][], DBDCollection> make3d = values ->
            new JDBCCollection(session.getProgressMonitor(), arrayStringItemType, PostgreArrayValueHandler.INSTANCE, Arrays.stream(values).map(make2d).toArray());

        final BiConsumer<String, DBDCollection> tester = (expected, collection) ->
            Assertions.assertEquals(expected, PostgreArrayValueHandler.INSTANCE.getValueDisplayString(collection.getComponentType(), collection, DBDDisplayFormat.UI));

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
            Assertions.assertEquals(
                expected,
                PostgreArrayValueHandler.INSTANCE.getValueDisplayString(collection.getComponentType(), collection, DBDDisplayFormat.NATIVE)
            );

        tester.accept("NULL", make2d.apply(null));
        tester.accept("{NULL,\"NULL\"}", make2d.apply(new String[] {null, "NULL"}));

        tester.accept("{}", make2d.apply(new String[] {}));
        tester.accept("{one,two,\" four with spaces \",\"f{i,v}e\"}",
            make2d.apply(new String[] {"one", "two", " four with spaces ", "f{i,v}e"})
        );

        tester.accept("{\"\\\"quote\\\"\"}", make2d.apply(new String[] {"\"quote\""}));

        tester.accept("{{a,b,c},{d,e,f}}", make3d.apply(new String[][] {{"a", "b", "c"}, {"d", "e", "f"}}));
        tester.accept("{{\"\"},{\"{}\"},{\"\\\"\"}}", make3d.apply(new String[][] {{""}, {"{}"}, {"\""}}));
    }

    @Test
    public void parseArrayString() throws DBCException {
        List<String> stringList = new ArrayList<>();
        stringList.add("A");
        stringList.add("B");
        Assertions.assertEquals(stringList, PostgreValueParser.parseArrayString("{\"A\",\"B\"}", ","));
        Assertions.assertNotEquals(stringList, PostgreValueParser.parseArrayString("{\"A\",\"B\",\"C\"}", ","));
        Assertions.assertNotEquals(stringList, PostgreValueParser.parseArrayString("{\"A\",\"B\"}", "."));

        List<String> intList = new ArrayList<>();
        intList.add("1");
        intList.add("22");
        intList.add("333");
        Assertions.assertEquals(intList, PostgreValueParser.parseArrayString("{1,22,333}", ","));
        //Assertions.assertEquals(intList, PostgreValueParser.parseArrayString("ARRAY[1,22,333]", ","));// todo: add array format support

        List<String> doublesList = new ArrayList<>();
        doublesList.add("1.123");
        doublesList.add("2.1421324124421");
        Assertions.assertEquals(doublesList, PostgreValueParser.parseArrayString("{1.123,2.1421324124421}", ","));
        Assertions.assertNotEquals(doublesList, PostgreValueParser.parseArrayString("{1.123,2.1421324124421}", "."));
//        Assertions.assertEquals(doublesList, PostgreValueParser.parseArrayString("ARRAY[1.123,2.1421324124421]", ","));

        //Infinity, -Infinity, NaN //todo

        List<String> intNullList = new ArrayList<>(intList);
        intNullList.add(null);
        Assertions.assertEquals(intNullList, PostgreValueParser.parseArrayString("{1,22,333,NULL}", ","));

        List<List<String>> int2List = new ArrayList<>();
        int2List.add(intList);
        int2List.add(intList);
        Assertions.assertEquals(int2List, PostgreValueParser.parseArrayString("{{1,22,333},{1,22,333}}", ","));
        Assertions.assertEquals(int2List, PostgreValueParser.parseArrayString("[1:2]={{1,22,333},{1,22,333}}", ",")); // "[1:2]=" do nothing
//        Assertions.assertEquals(int2List, PostgreValueParser.parseArrayString("ARRAY[[1,22,333],[1,22,333]]", ","));

        List<List<List<String>>> int3List = new ArrayList<>();
        int3List.add(int2List);
        int3List.add(int2List);
        Assertions.assertEquals(int3List, PostgreValueParser.parseArrayString("{{{1,22,333},{1,22,333}},{{1,22,333},{1,22,333}}}", ","));

    }

    @Test
    public void testParsePrimitiveArray() {
        Assertions.assertArrayEquals(
            new String[]{},
            PostgreValueParser.parsePrimitiveArray("{}", Function.identity(), String[]::new));
        Assertions.assertArrayEquals(
            new String[]{null, "NULL"},
            PostgreValueParser.parsePrimitiveArray("{NULL,\"NULL\"}", Function.identity(), String[]::new));
        Assertions.assertArrayEquals(
            new String[]{"ab", "cd", null, "NULL", " spa ce "},
            PostgreValueParser.parsePrimitiveArray("{ ab , cd ,NULL,\"NULL\",\" spa ce \"}", Function.identity(), String[]::new));
        Assertions.assertArrayEquals(
            new Integer[]{1, null, 3},
            PostgreValueParser.parsePrimitiveArray("{1,NULL,3}", Integer::valueOf, Integer[]::new));

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> PostgreValueParser.parsePrimitiveArray("1}", Function.identity(), String[]::new),
            "Array value must start with \"{\"");
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> PostgreValueParser.parsePrimitiveArray("{1,}", Function.identity(), String[]::new),
            "Unexpected \"}\" character");
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> PostgreValueParser.parsePrimitiveArray("{,}", Function.identity(), String[]::new),
            "Unexpected \",\" character");
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> PostgreValueParser.parsePrimitiveArray("{1,", Function.identity(), String[]::new),
            "Unexpected end of input");
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> PostgreValueParser.parsePrimitiveArray("{1},", Function.identity(), String[]::new),
            "Junk after closing right brace");
    }

    private void setupCompositeArrayMocks() throws DBException {
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(container.getDriver()).thenReturn(driver);
        Mockito.when(driver.getProviderDescriptor()).thenReturn(providerDescriptor);
        Mockito.when(providerDescriptor.getId()).thenReturn("postgresql");
        Mockito.when(dataSource.getServerType()).thenReturn(serverType);

        setupCompositeType();
        Mockito.when(compositeArrayType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
        Mockito.when(compositeArrayType.getComponentType(session.getProgressMonitor())).thenReturn(outerCompositeType);
        Mockito.when(compositeArrayType.getArrayDelimiter()).thenReturn(",");
        Mockito.when(outerCompositeType.getArrayDelimiter()).thenReturn(",");
    }

    @NotNull
    private Object[] parseCompositeArray(@NotNull String value) throws DBException {
        Object parsedValue = PostgreValueParser.convertStringToValue(session, compositeArrayType, value);
        return Assertions.assertInstanceOf(Object[].class, parsedValue);
    }

    @NotNull
    private String getArrayDisplayString(@NotNull Object[] array, @NotNull DBDDisplayFormat format) {
        JDBCCollection collection = new JDBCCollection(
            session.getProgressMonitor(), outerCompositeType, DBUtils.findValueHandler(session, outerCompositeType), array);
        return PostgreArrayValueHandler.INSTANCE.getValueDisplayString(compositeArrayType, collection, format);
    }

    @NotNull
    private static String createArrayElement(int originX, int originY, int sizeX, int sizeY) {
        return "\"(\\\"(" + originX + "," + originY + ")\\\",\\\"(" + sizeX + "," + sizeY + ")\\\")\"";
    }

    private static void assertRecti(@NotNull Object value, int originX, int originY, int sizeX, int sizeY) {
        JDBCComposite recti = Assertions.assertInstanceOf(JDBCComposite.class, value);
        JDBCComposite origin = Assertions.assertInstanceOf(JDBCComposite.class, recti.getValues()[0]);
        JDBCComposite size = Assertions.assertInstanceOf(JDBCComposite.class, recti.getValues()[1]);
        Assertions.assertArrayEquals(new Object[]{originX, originY}, origin.getValues());
        Assertions.assertArrayEquals(new Object[]{sizeX, sizeY}, size.getValues());
    }

    private void setupCompositeType() throws DBException {
        Mockito.when(innerCompositeType.getDataSource()).thenReturn(dataSource);
        Mockito.when(innerCompositeType.getDataKind()).thenReturn(DBPDataKind.STRUCT);
        Mockito.when(innerCompositeType.getTypeID()).thenReturn(Types.STRUCT);
        Mockito.when(innerCompositeType.getTypeName()).thenReturn("vec2i");
        Mockito.when(innerCompositeType.getAttributes(session.getProgressMonitor())).thenReturn(
            List.of(innerFirstAttribute, innerSecondAttribute));

        Mockito.when(outerCompositeType.getDataSource()).thenReturn(dataSource);
        Mockito.when(outerCompositeType.getDataKind()).thenReturn(DBPDataKind.STRUCT);
        Mockito.when(outerCompositeType.getTypeID()).thenReturn(Types.STRUCT);
        Mockito.when(outerCompositeType.getTypeName()).thenReturn("recti");
        Mockito.when(outerCompositeType.getAttributes(session.getProgressMonitor())).thenReturn(
            List.of(outerFirstAttribute, outerSecondAttribute));

        setupIntAttribute(innerFirstAttribute, "x", 0);
        setupIntAttribute(innerSecondAttribute, "y", 1);
        setupCompositeAttribute(outerFirstAttribute, "origin", 0);
        setupCompositeAttribute(outerSecondAttribute, "size", 1);

        Mockito.when(outerFirstAttribute.getDataType()).thenReturn(innerCompositeType);
        Mockito.when(outerSecondAttribute.getDataType()).thenReturn(innerCompositeType);
    }

    private void setupIntAttribute(@NotNull PostgreDataTypeAttribute attribute, @NotNull String name, int position) {
        Mockito.when(attribute.getName()).thenReturn(name);
        Mockito.when(attribute.getOrdinalPosition()).thenReturn(position);
        Mockito.when(attribute.getDataKind()).thenReturn(DBPDataKind.NUMERIC);
        Mockito.when(attribute.getTypeID()).thenReturn(Types.INTEGER);
        Mockito.when(attribute.getTypeName()).thenReturn("int4");
    }

    private void setupCompositeAttribute(@NotNull PostgreDataTypeAttribute attribute, @NotNull String name, int position) {
        Mockito.when(attribute.getName()).thenReturn(name);
        Mockito.when(attribute.getOrdinalPosition()).thenReturn(position);
        Mockito.when(attribute.getDataKind()).thenReturn(DBPDataKind.STRUCT);
        Mockito.when(attribute.getTypeID()).thenReturn(Types.STRUCT);
        Mockito.when(attribute.getTypeName()).thenReturn("vec2i");
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

