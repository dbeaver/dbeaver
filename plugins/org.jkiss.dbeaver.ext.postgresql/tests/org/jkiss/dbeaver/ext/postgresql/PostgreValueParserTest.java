/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PostgreValueParserTest {

    @Mock
    private DBCSession session;

    @Mock
    private DBSDataType arrayDoubleItemType;

    @Mock
    private DBSDataType array_2_DoubleItemType; //multidimensional

    @Mock
    private DBSDataType arrayIntItemType;

    @Mock
    private DBSDataType arrayStringItemType;

    @Mock
    private DBSDataType arrayBooleanItemType;

    @Mock
    private DBSDataType arrayStructItemType;

    @Mock
    private DBSDataType stringItemType;

    @Mock
    private DBSDataType intItemType;

    @Mock
    private DBSDataType booleanItemType;

    @Mock
    private DBSDataType doubleItemType;

    @Mock
    private DBSDataType structItemType;

    @Before
    public void setUp() throws DBException {
        setupGeneralWhenMocks();
    }

    @Test
    public void convertStringToValue() throws DBCException {
        Assert.assertEquals(1, PostgreValueParser.convertStringToValue(session, intItemType, "1", true));
        Assert.assertEquals(1.111, PostgreValueParser.convertStringToValue(session, doubleItemType, "1.111", true));
        Assert.assertArrayEquals(new String[]{"A", "B"},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayStringItemType, "{\"A\",\"B\"}", true));
        Assert.assertArrayEquals(new Integer[]{1, 22},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayIntItemType, "{1,22}", true));
        Assert.assertArrayEquals(new Double[]{1.1, 22.22},
                (Object[]) PostgreValueParser.convertStringToValue(session, arrayDoubleItemType, "{1.1,22.22}", true));
        // todo: is not working with multidimensional arrays
//        Assert.assertArrayEquals(new Double[][]{new Double[]{1.1, 22.22}, new Double[]{3.33, 44.44}},
//                (Object[][]) PostgreValueParser.convertStringToValue(session, array_2_DoubleItemType, "{{1.1,22.22},{3.33,44.44}}", true));

        Boolean[] booleans = {true, false};
        Assert.assertEquals(true, PostgreValueParser.convertStringToValue(session, booleanItemType, "true", true));

        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType, "{TRUE,FALSE}", true));

        //todo: add support alternatives to "true/false"
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType, "{'t','f'}", true));
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType, "{'true','false'}", true));
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'1','0'}", true));
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'y','n'}", true));
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'yes,'no'}", true));
//        Assert.assertArrayEquals(booleans, (Object[]) PostgreValueParser.convertStringToValue(session, arrayBooleanItemType,"{'on,'off'}", true));

        // todo: is not working with structures
//        Assert.assertEquals("(\"fuzzy dice\",42,1.99)",
//                PostgreValueParser.convertStringToValue(session, structItemType, "(\"fuzzy dice\",42,1.99)", true));
//        Assert.assertArrayEquals(new String[]{"(\"fuzzy dice\",42,1.99)", "(\"apple\",1,2222.12345)"},
//                (Object[]) PostgreValueParser.convertStringToValue(session, arrayStructItemType, "{(\"fuzzy dice\",42,1.99), (\"apple\",1,2222.12345)}", true));

    }

    @Test
    public void parseObjectString() throws DBCException {
        Assert.assertArrayEquals(new String[]{}, PostgreValueParser.parseObjectString(""));
        Assert.assertArrayEquals(new String[]{"colA", " ColB"}, PostgreValueParser.parseObjectString("colA, ColB"));
        Assert.assertArrayEquals(new String[]{"A", " B"}, PostgreValueParser.parseObjectString("A, B"));
    }

    @Test
    public void generateObjectString() throws DBCException {
        Assert.assertEquals("(\"A\",\"B\")", PostgreValueParser.generateObjectString(new String[]{"A", "B"}));
        //todo: unquote numbers
        Assert.assertEquals("(\"1\",\"2\",\"3\")", PostgreValueParser.generateObjectString(new Integer[]{1, 2, 3}));
        Assert.assertEquals("(\"1.1\",\"2.22\",\"3.333\")", PostgreValueParser.generateObjectString(new Double[]{1.1, 2.22, 3.333}));
        Assert.assertEquals("(\"(1,2,3)\")", PostgreValueParser.generateObjectString(new Object[]{"(1,2,3)"}));
        Assert.assertEquals("(\"(1,2,3)\",\"(4,5,6)\")", PostgreValueParser.generateObjectString(new Object[]{"(1,2,3)","(4,5,6)"}));

        // todo: is not working with multidimensional arrays
//        Assert.assertEquals("({\"1.1\",\"2.22\",\"3.333\"}, {\"1.1\",\"2.22\",\"3.333\"})",
//                PostgreValueParser.generateObjectString(new Double[][]{{1.1, 2.22, 3.333},{1.1, 2.22, 3.333}}));
//        Assert.assertEquals("({\"(1,2,3)\",\"(4,5,6)\"},{\"(1,2,3)\",\"(4,5,6)\"})",
//                PostgreValueParser.generateObjectString(new Object[][]{new String[]{"(1,2,3)","(4,5,6)"},new String[]{"(1,2,3)","(4,5,6)"}}));
    }

    @Test
    public void parseArrayString() throws DBCException {
        List<String> stringList = new ArrayList<>();
        stringList.add("A");
        stringList.add("B");
        Assert.assertEquals(stringList, PostgreValueParser.parseArrayString("{\"A\",\"B\"}", ","));

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
    }

    private void setupGeneralWhenMocks() throws DBException {
        Mockito.when(session.getProgressMonitor()).thenReturn(new VoidProgressMonitor());

        Mockito.when(intItemType.getFullTypeName()).thenReturn("test_intItemType");
        Mockito.when(intItemType.getDataKind()).thenReturn(DBPDataKind.ANY);
        Mockito.when(intItemType.getTypeID()).thenReturn(Types.INTEGER);

        Mockito.when(arrayIntItemType.getFullTypeName()).thenReturn("test_arrayIntItemType");
        Mockito.when(arrayIntItemType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
        Mockito.when(arrayIntItemType.getComponentType(session.getProgressMonitor())).thenReturn(intItemType);

        Mockito.when(doubleItemType.getFullTypeName()).thenReturn("test_doubleItemType");
        Mockito.when(doubleItemType.getDataKind()).thenReturn(DBPDataKind.NUMERIC);
        Mockito.when(doubleItemType.getTypeID()).thenReturn(Types.DOUBLE);

        Mockito.when(arrayDoubleItemType.getFullTypeName()).thenReturn("test_arrayDoubleItemType");
        Mockito.when(arrayDoubleItemType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
        Mockito.when(arrayDoubleItemType.getComponentType(session.getProgressMonitor())).thenReturn(doubleItemType);

        Mockito.when(array_2_DoubleItemType.getFullTypeName()).thenReturn("test_array_2_DoubleItemType");
        Mockito.when(array_2_DoubleItemType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
        Mockito.when(array_2_DoubleItemType.getComponentType(session.getProgressMonitor())).thenReturn(arrayDoubleItemType);

        Mockito.when(stringItemType.getFullTypeName()).thenReturn("test_stringItemType");
        Mockito.when(stringItemType.getDataKind()).thenReturn(DBPDataKind.STRING);
        Mockito.when(stringItemType.getTypeID()).thenReturn(Types.VARCHAR);

        Mockito.when(arrayStringItemType.getFullTypeName()).thenReturn("test_arrayStringItemType");
        Mockito.when(arrayStringItemType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
        Mockito.when(arrayStringItemType.getComponentType(session.getProgressMonitor())).thenReturn(stringItemType);

        Mockito.when(structItemType.getFullTypeName()).thenReturn("test_structItemType");
        Mockito.when(structItemType.getDataKind()).thenReturn(DBPDataKind.STRUCT);
        Mockito.when(structItemType.getTypeID()).thenReturn(Types.STRUCT);

        Mockito.when(arrayStructItemType.getFullTypeName()).thenReturn("test_arrayStructItemType");
        Mockito.when(arrayStructItemType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
        Mockito.when(arrayStructItemType.getComponentType(session.getProgressMonitor())).thenReturn(structItemType);

        Mockito.when(booleanItemType.getFullTypeName()).thenReturn("test_booleanItemType");
        Mockito.when(booleanItemType.getDataKind()).thenReturn(DBPDataKind.BOOLEAN);
        Mockito.when(booleanItemType.getTypeID()).thenReturn(Types.BOOLEAN);

        Mockito.when(arrayBooleanItemType.getFullTypeName()).thenReturn("test_arrayBooleanItemType");
        Mockito.when(arrayBooleanItemType.getDataKind()).thenReturn(DBPDataKind.ARRAY);
        Mockito.when(arrayBooleanItemType.getComponentType(session.getProgressMonitor())).thenReturn(booleanItemType);

    }
}