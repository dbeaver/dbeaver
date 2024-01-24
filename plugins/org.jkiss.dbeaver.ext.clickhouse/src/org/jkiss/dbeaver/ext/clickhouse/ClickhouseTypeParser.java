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
package org.jkiss.dbeaver.ext.clickhouse;

import com.google.gson.Gson;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.clickhouse.ClickhouseDataTypesParser.ArrayTypeContext;
import org.jkiss.dbeaver.ext.clickhouse.ClickhouseDataTypesParser.MapTypeContext;
import org.jkiss.dbeaver.ext.clickhouse.ClickhouseDataTypesParser.TupleTypeContext;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseArrayType;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseDataSource;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseMapType;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseTupleType;
import org.jkiss.dbeaver.ext.clickhouse.model.data.ClickhouseTupleValue;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCompositeMap;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClickhouseTypeParser {
    private static final Log log = Log.getLog(ClickhouseTypeParser.class);

    private static final Gson gson = new Gson();

    private ClickhouseTypeParser() {
        // prevents instantiation
    }

    public static boolean isComplexType(@NotNull String typeName) {
        return typeName.startsWith("Map") || typeName.startsWith("Tuple") || typeName.startsWith("Array");
    }

    @Nullable
    public static Object makeValue(@NotNull DBCSession session, @NotNull String typeName, @Nullable Object object) throws DBException {
        final DBSDataType type = getType(session.getProgressMonitor(), (ClickhouseDataSource) session.getDataSource(), typeName);

        if (type != null) {
            return makeValue(session, type, object);
        } else {
            return object;
        }
    }

    @Nullable
    public static Object makeValue(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object) throws DBException {
        if (object == null) {
            return null;
        }

        if (type instanceof ClickhouseMapType map) {
            return new JDBCCompositeMap(session, map, ((Map<?, ?>) object));
        } else if (type instanceof ClickhouseTupleType tuple) {
            final Object[] values;
            if (object instanceof Map) {
                values = ((Map<?, ?>) object).entrySet().stream()
                    .flatMap(e -> Stream.of(e.getKey(), e.getValue())).toArray();
            } else if (object instanceof String) { 
                values = JSONUtils.parseMap(gson, new StringReader((String) object)).entrySet().stream()
                    .flatMap(e -> Stream.of(e.getKey(), e.getValue())).toArray();
            } else {
                values = ((Collection<?>) object).toArray();
            }

            for (int i = 0; i < values.length; i++) {
                values[i] = makeValue(session, tuple.getAttributes().get(i).getDataType(), values[i]);
            }

            return new ClickhouseTupleValue(session.getProgressMonitor(), tuple, values);
        } else {
            return object;
        }
    }

    @Nullable
    public static DBSDataType getType(@NotNull DBRProgressMonitor monitor, @NotNull ClickhouseDataSource dataSource, @NotNull String typeName) throws DBException {
        final var lexer = new ClickhouseDataTypesLexer(CharStreams.fromString(typeName));
        final var parser = new ClickhouseDataTypesParser(new CommonTokenStream(lexer));
        final var type = parser.type().anyType();
        final DBSDataType resolved;

        if (parser.getNumberOfSyntaxErrors() > 0) {
            log.debug("Rejecting invalid or unsupported type: " + typeName);
            return null;
        }

        if (type.simpleType() != null) {
            resolved = DBUtils.resolveDataType(monitor, dataSource, type.simpleType().getText());
        } else if (type.markerType() != null) {
            resolved = DBUtils.resolveDataType(monitor, dataSource, type.markerType().anyType().getText());
        } else if (type.tupleType() != null) {
            resolved = getTupleType(monitor, dataSource, type.tupleType());
        } else if (type.mapType() != null) {
            resolved = getMapType(monitor, dataSource, type.mapType());
        } else if (type.arrayType() != null) {
            resolved = getArrayType(monitor, dataSource, type.arrayType());
        } else {
            resolved = null;
        }

        if (resolved == null) {
            log.debug("Can't resolve type for '" + typeName + "'");
        }

        return resolved;
    }

    @Nullable
    private static DBSDataType getArrayType(@NotNull DBRProgressMonitor monitor, @NotNull ClickhouseDataSource dataSource, ArrayTypeContext type) throws DBException {
        final DBSDataType componentType = getType(monitor, dataSource, type.anyType().getText());

        if (componentType == null) {
            return null;
        }

        return new ClickhouseArrayType(dataSource, componentType);
    }

    @Nullable
    private static DBSDataType getTupleType(@NotNull DBRProgressMonitor monitor, @NotNull ClickhouseDataSource dataSource, @NotNull TupleTypeContext context) throws DBException {
        final List<String> names = new ArrayList<>();
        final List<DBSDataType> types = new ArrayList<>();

        if (context.tupleElementList() != null && context.tupleElementList().tupleElement() != null) {
            for (ClickhouseDataTypesParser.TupleElementContext element : context.tupleElementList().tupleElement()) {
                final DBSDataType type = getType(monitor, dataSource, element.anyType().getText());

                if (type == null) {
                    return null;
                }

                names.add("Attr" + (names.size() + 1)); // TODO: Named tuple elements are not supported
                types.add(type);
            }
        }

        return new ClickhouseTupleType(
            dataSource,
            types.toArray(DBSDataType[]::new),
            names.toArray(String[]::new)
        );
    }

    @Nullable
    private static DBSDataType getMapType(@NotNull DBRProgressMonitor monitor, @NotNull ClickhouseDataSource dataSource, @NotNull MapTypeContext context) throws DBException {
        final DBSDataType keyType = getType(monitor, dataSource, context.key.getText());
        final DBSDataType valueType = getType(monitor, dataSource, context.value.getText());

        if (keyType == null || valueType == null) {
            return null;
        }

        return new ClickhouseMapType(dataSource, keyType, valueType);
    }

    @NotNull
    public static Map<String, Integer> tryParseEnumEntries(@NotNull String type) {
        if (!isEnum(type)) {
            return Collections.emptyMap();
        }
        var input = CharStreams.fromString(type);
        var ll = new ClickhouseDataTypesLexer(input);
        var tokens = new CommonTokenStream(ll);
        var pp = new ClickhouseDataTypesParser(tokens);
        var tree = pp.enumType();

        if (tree.enumEntryList() != null && tree.enumEntryList().enumEntry() != null) {
            return tree.enumEntryList().enumEntry().stream()
                .filter(node -> node != null && node.String() != null && node.Number() != null)
                .map(node -> {
                    final var stringValue = node.String().getText();
                    final var key = stringValue.substring(1, stringValue.length() - 1);
                    final var val = CommonUtils.toInt(node.Number().getText());
                    return new Pair<>(key, val);
                })
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        }

        return Collections.emptyMap();
    }

    @NotNull
    public static String getTypeNameWithoutModifiers(@NotNull String fullTypeName) {
        final int div = fullTypeName.indexOf('(');
        if (div < 0) {
            return fullTypeName;
        } else {
            return fullTypeName.substring(0, div);
        }
    }

    private static boolean isEnum(@NotNull String type) {
        final String name = type.toLowerCase();
        return name.startsWith("enum8(") || name.startsWith("enum16(");
    }
}
