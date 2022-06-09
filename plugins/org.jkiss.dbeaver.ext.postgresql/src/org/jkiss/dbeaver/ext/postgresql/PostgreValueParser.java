/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTypeType;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCComposite;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.csv.CSVReaderBuilder;
import org.jkiss.utils.csv.CSVReaderNullFieldIndicator;
import org.jkiss.utils.csv.CSVWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PostgreValueParser {

    private static final Log log = Log.getLog(PostgreValueParser.class);

    public static Object convertStringToValue(DBCSession session, DBSTypedObject arrayType, String string) throws DBCException {
        if (arrayType.getDataKind() == DBPDataKind.ARRAY) {
            if (string != null && string.startsWith("{") && string.endsWith("}")) {
                try {
                    return prepareToParseArray(session, arrayType, string);
                } catch (Exception e) {
                    log.error("Array parsing failed " + e.getMessage());
                    return string;
                }
            } else {
                //log.error("Unsupported array string: '" + string + "'");
                // It can be already a string object as an element of parsed array
                return string;
            }
        }
        if (CommonUtils.isEmpty(string)) {
            return convertStringToSimpleValue(session, arrayType, string);
        }
        try {
            switch (arrayType.getTypeID()) {
                case Types.BOOLEAN:
                    return string.length() > 0 && Character.toLowerCase(string.charAt(0)) == 't'; //todo: add support of alternatives to "true/false"
                case Types.TINYINT:
                    return Byte.parseByte(string);
                case Types.SMALLINT:
                    return Short.parseShort(string);
                case Types.INTEGER:
                    return Integer.parseInt(string);
                case Types.BIGINT:
                    return Long.parseLong(string);
                case Types.FLOAT:
                    return Float.parseFloat(string);
                case Types.REAL:
                case Types.NUMERIC:
                case Types.DOUBLE:
                    return Double.parseDouble(string);
                default: {
                    return convertStringToSimpleValue(session, arrayType, string);
                }
            }
        } catch (NumberFormatException e) {
            return string;
        }
    }

    private static Object prepareToParseArray(DBCSession session, DBSTypedObject arrayType, String string) throws DBCException {
        DBSDataType arrayDataType = arrayType instanceof DBSDataType ? (DBSDataType) arrayType : ((DBSTypedObjectEx) arrayType).getDataType();
        try {
            if (arrayDataType == null) {
                log.error("Can't get array type '" + arrayType.getFullTypeName() + "'");
                return string;
            }
            DBSDataType componentType = arrayDataType.getComponentType(session.getProgressMonitor());
            if (componentType == null && arrayType instanceof PostgreDataType && ((PostgreDataType) arrayType).getTypeType() == PostgreTypeType.d) {
                // Domains store component type information in another field
                componentType = ((PostgreDataType) arrayType).getBaseType(session.getProgressMonitor());
            }
            if (componentType == null) {
                log.error("Can't get component type from array '" + arrayType.getFullTypeName() + "'");
                return string;
            } else {
                if (componentType instanceof PostgreDataType) {
                    List<Object> itemStrings = parseArrayString(string, ",");
                    return startTransformListOfValuesIntoArray(session, (PostgreDataType)componentType, itemStrings);
                } else {
                    log.error("Incorrect type '" + arrayType.getFullTypeName() + "'");
                    return string;
                }
            }
        } catch (Exception e) {
            throw new DBCException("Error extracting array '" + arrayType.getFullTypeName() + "' items", e);
        }
    }

    private static Object startTransformListOfValuesIntoArray(DBCSession session, PostgreDataType itemType, List list) throws DBCException {
        //If array is one dimensional, we will return array of that type. If array is multidimensional we will return array of JDBCCollections.
        return transformListOfValuesIntoArray(session, itemType, list, true);
    }

    private static Object transformListOfValuesIntoArray(DBCSession session, PostgreDataType itemType, List list, boolean firstAttempt) throws DBCException { //transform into array
        Object[] values = new Object[list.size()];
        for (int index = 0; index < list.size(); index++) {
            if (list.get(index) instanceof List) {
                values[index] = transformListOfValuesIntoArray(session, itemType, (List) list.get(index), false);
            } else {
                Object[] itemValues = new Object[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    itemValues[i] = convertStringToValue(session, itemType, (String) list.get(i));
                }
                if(firstAttempt){
                    return itemValues;
                } else {
                    return new JDBCCollection(session.getProgressMonitor(), itemType, DBUtils.findValueHandler(session, itemType), itemValues);
                }
            }
        }
        if (firstAttempt) {
            return values;
        } else {
            return new JDBCCollection(session.getProgressMonitor(), itemType, DBUtils.findValueHandler(session, itemType), values);

        }
    }

    private static Object convertStringToSimpleValue(DBCSession session, DBSTypedObject itemType, String string) throws DBCException {
        DBDValueHandler valueHandler = DBUtils.findValueHandler(session, itemType);
        if (valueHandler != null) {
            return valueHandler.getValueFromObject(session, itemType, string, false, false);
        } else {
            return string;
        }
    }

    public static String[] parseSingleObject(String string) throws DBCException { //only for objects(structures), not for arrays
        if (string.isEmpty()) {
            return new String[0];
        }
        try {
            // Empty separators are NULLs, empty quotes are empty strings.
            // https://www.postgresql.org/docs/current/rowtypes.html#id-1.5.7.24.6
            return new CSVReaderBuilder(new StringReader(string))
                .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
                .build()
                .readNext();
        } catch (IOException e) {
            throw new DBCException("Error parsing PGObject", e);
        }
    }

    public static String generateObjectString(Object[] values) {
        String[] line = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value instanceof DBDCollection) {
                value = ((DBDCollection) value).getRawValue();
            }
            if (value instanceof Object[]) {
                String arrayPostgreStyle = Arrays.deepToString((Object[]) value)
                        .replace("[", "{")
                        .replace("]", "}")
                        .replace(" ", "");
                line[i] = arrayPostgreStyle; //Strings are not quoted
            } else if (value instanceof JDBCComposite) {
                line[i] = generateObjectString(((JDBCComposite) value).getValues());
            } else if (value != null) {
                // Values are simply skipped if they're NULL.
                // https://www.postgresql.org/docs/current/rowtypes.html#id-1.5.7.24.6
                line[i] = value.toString();
            }
        }
        StringWriter out = new StringWriter();
        final CSVWriter writer = new CSVWriter(out);
        writer.writeNext(line);
        try {
            writer.flush();
        } catch (IOException e) {
            log.warn(e);
        }
        return "(" + out.toString().trim() + ")";
    }

    // Copied from pgjdbc array parser class
    // https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/jdbc/PgArray.java
    public static List<Object> parseArrayString(String fieldString, String delimiter) throws DBCException {
        List<Object> arrayList = new ArrayList<>();
        if (CommonUtils.isEmpty(fieldString)) {
            return arrayList;
        }

        int dimensionsCount = 1;
        char delim = delimiter.charAt(0);//connection.getTypeInfo().getArrayDelimiter(oid);

        if (fieldString != null) {
            int bracePairsCount = 0;
            char[] chars = fieldString.toCharArray();
            StringBuilder buffer = null;
            boolean insideString = false;
            boolean wasInsideString = false; // needed for checking if NULL
            // value occurred
            List<List<Object>> dims = new ArrayList<>(); // array dimension arrays
            List<Object> curArray = arrayList; // currently processed array

            // Starting with 8.0 non-standard (beginning index
            // isn't 1) bounds the dimensions are returned in the
            // data formatted like so "[0:3]={0,1,2,3,4}".
            // Older versions simply do not return the bounds.
            //
            // Right now we ignore these bounds, but we could
            // consider allowing these index values to be used
            // even though the JDBC spec says 1 is the first
            // index. I'm not sure what a client would like
            // to see, so we just retain the old behavior.
            int startOffset = 0;
            {
                if (chars[0] == '[') {
                    while (chars[startOffset] != '=') {
                        startOffset++;
                    }
                    startOffset++; // skip =
                }
            }

            for (int i = startOffset; i < chars.length; i++) {

                // escape character that we need to skip
                if (chars[i] == '\\') {
                    i++;
                } else if (!insideString && chars[i] == '{') {
                    // subarray start
                    if (dims.isEmpty()) {
                        dims.add(arrayList);
                    } else {
                        List<Object> a = new ArrayList<>();
                        List<Object> p = dims.get(dims.size() - 1);
                        p.add(a);
                        dims.add(a);
                    }
                    bracePairsCount++;
                    curArray = dims.get(dims.size() - 1);

                    // number of dimensions
                    {
                        for (int t = i + 1; t < chars.length; t++) {
                            if (Character.isWhitespace(chars[t])) {
                                continue;
                            } else if (chars[t] == '{') {
                                dimensionsCount++;
                            } else {
                                break;
                            }
                        }
                    }

                    buffer = new StringBuilder();
                    continue;
                } else if (chars[i] == '"') {
                    // quoted element
                    insideString = !insideString;
                    wasInsideString = true;
                    continue;
                } else if (!insideString && Character.isWhitespace(chars[i])) {
                    // white space
                    continue;
                } else if ((!insideString && (chars[i] == delim || chars[i] == '}'))
                    || i == chars.length - 1) {
                    // array end or element end
                    // when character that is a part of array element
                    if (chars[i] != '"' && chars[i] != '}' && chars[i] != delim && buffer != null) {
                        buffer.append(chars[i]);
                    }

                    String b = buffer == null ? null : buffer.toString();

                    // add element to current array
                    if (b != null && (!b.isEmpty() || wasInsideString)) {
                        curArray.add(!wasInsideString && b.equals("NULL") ? null : b);
                    }

                    wasInsideString = false;
                    buffer = new StringBuilder();

                    // when end of an array
                    if (chars[i] == '}') {
                        dims.remove(dims.size() - 1);
                        bracePairsCount--;

                        // when multi-dimension
                        if (!dims.isEmpty()) {
                            curArray = dims.get(dims.size() - 1);
                        }

                        buffer = null;
                    }

                    continue;
                }

                if (buffer != null) {
                    buffer.append(chars[i]);
                }
            }
            if (bracePairsCount != 0) {
                throw new DBCException("Amount of array's braces is not equal");
            }
        }
        return arrayList;
    }
}
