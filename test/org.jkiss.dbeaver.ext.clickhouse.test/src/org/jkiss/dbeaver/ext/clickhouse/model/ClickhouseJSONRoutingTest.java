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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.clickhouse.model.data.ClickhouseArrayValueHandler;
import org.jkiss.dbeaver.ext.clickhouse.model.data.ClickhouseContentJSON;
import org.jkiss.dbeaver.ext.clickhouse.model.data.ClickhouseJSONValueHandler;
import org.jkiss.dbeaver.ext.clickhouse.model.data.ClickhouseStructValueHandler;
import org.jkiss.dbeaver.ext.clickhouse.model.data.ClickhouseValueHandlerProvider;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.registry.datatype.ValueHandlerDescriptor;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Scalar JSON variants use the existing content viewer without changing collection or binding behavior. */
public class ClickhouseJSONRoutingTest extends DBeaverUnitTest {
    private final ClickhouseValueHandlerProvider provider = new ClickhouseValueHandlerProvider();

    @NotNull
    private static Stream<String> supportedJsonTypes() {
        return Stream.of(
            "JSON", "json", "JSON()", "JSON( )", "Nullable(JSON)", "nullable(json)", "Nullable(JSON())",
            "JSON(a UInt64)", "json(a UInt64)", "JSON(max_dynamic_types=1, max_dynamic_paths=0)",
            "JSON(a Array(Nullable(UInt64)))", "JSON(a Tuple(x UInt64, y String))", "JSON(a Map(String, UInt64))",
            "JSON(a JSON)", "JSON(a Decimal(38, 9))", "JSON(SKIP a.b)", "JSON(SKIP REGEXP '^(foo|bar)$')",
            "JSON(SKIP REGEXP '[)]')", "JSON(`a)b` UInt64)", "JSON(`a(b` UInt64)", "JSON(\"a)b\" UInt64)",
            "JSON(`a``)b` UInt64)", "JSON(a Enum8('x(y)'=1, 'z'=2))", "JSON(a Enum8('x\\')y'=1))",
            "Nullable(JSON(a UInt64))", "Nullable(JSON(a Array(Nullable(UInt64))))",
            "Nullable(JSON(SKIP REGEXP '[)]'))", "Nullable(JSON(`a(b` UInt64))",
            // JDBC v2 0.9.8/0.10.0 omit only the outer Nullable close in result-set metadata.
            "Nullable(JSON()", "Nullable(JSON(a UInt64)", "Nullable(JSON(a Array(Nullable(UInt64)))",
            "Nullable(JSON(SKIP REGEXP '[)]')", "Nullable(JSON(`a)b` UInt64)"
        );
    }

    @NotNull
    private DBSTypedObject type(@NotNull String name, @NotNull DBPDataKind kind) {
        DBSTypedObject type = mock(DBSTypedObject.class);
        when(type.getTypeName()).thenReturn(name);
        when(type.getDataKind()).thenReturn(kind);
        when(type.getTypeID()).thenReturn(Types.OTHER);
        return type;
    }

    @Nullable
    private DBDValueHandler handler(@NotNull String name, @NotNull DBPDataKind kind) {
        return provider.getValueHandler(mock(DBPDataSource.class), mock(DBDFormatSettings.class), type(name, kind));
    }

    @ParameterizedTest
    @MethodSource("supportedJsonTypes")
    public void scalarJsonRoutingDoesNotDependOnMetadataKind(@NotNull String name) {
        for (DBPDataKind kind : new DBPDataKind[] {DBPDataKind.UNKNOWN, DBPDataKind.CONTENT, DBPDataKind.STRING}) {
            assertSame(ClickhouseJSONValueHandler.INSTANCE, handler(name, kind), kind.name());
        }
    }

    @ParameterizedTest
    @MethodSource("supportedJsonTypes")
    public void supportedJsonTypesUseContentKind(@NotNull String name) {
        var dataSource = mock(ClickhouseDataSource.class, CALLS_REAL_METHODS);
        doReturn(null).when(dataSource).getLocalDataType(anyString());
        assertEquals(DBPDataKind.CONTENT, dataSource.resolveDataKind(name, Types.OTHER));
        assertEquals(DBPDataKind.CONTENT, dataSource.resolveDataKind(name, Types.VARCHAR));
    }

    @ParameterizedTest
    @MethodSource("supportedJsonTypes")
    public void cachedJsonTypesUseContentKindInResultSets(@NotNull String name) throws Exception {
        var dataSource = dataSourceWithTypeCache(true);
        assertSame(dataSource.getLocalDataType("JSON"), dataSource.getLocalDataType(name));
        for (int jdbcType : new int[] {Types.OTHER, Types.VARCHAR}) {
            for (int nullable : new int[] {ResultSetMetaData.columnNoNulls, ResultSetMetaData.columnNullable}) {
                var jdbcMetadata = mock(ResultSetMetaData.class);
                when(jdbcMetadata.getColumnTypeName(1)).thenReturn(name);
                when(jdbcMetadata.getColumnType(1)).thenReturn(jdbcType);
                when(jdbcMetadata.isNullable(1)).thenReturn(nullable);
                var column = new JDBCColumnMetaData(dataSource, jdbcMetadata, 0);
                assertEquals(DBPDataKind.CONTENT, column.getDataKind());
                assertEquals(Types.CLOB, column.getTypeID());
                assertEquals(name, column.getTypeName());
                assertEquals(nullable == ResultSetMetaData.columnNoNulls, column.isRequired());
                assertSame(ClickhouseJSONValueHandler.INSTANCE,
                    provider.getValueHandler(dataSource, mock(DBDFormatSettings.class), column));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("supportedJsonTypes")
    public void emptyTypeCacheFallsBackToJsonContentKind(@NotNull String name) throws Exception {
        var dataSource = dataSourceWithTypeCache(false);
        var jdbcMetadata = mock(ResultSetMetaData.class);
        when(jdbcMetadata.getColumnTypeName(1)).thenReturn(name);
        when(jdbcMetadata.getColumnType(1)).thenReturn(Types.OTHER);
        assertNull(dataSource.getLocalDataType(name));
        assertEquals(DBPDataKind.CONTENT, new JDBCColumnMetaData(dataSource, jdbcMetadata, 0).getDataKind());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "Nullable(String)|Nullable", "Nullable(Array(JSON))|Nullable", "Nullable(Nullable(JSON))|Nullable",
        "Array(JSON)|Array", "Array(Nullable(JSON))|Array", "Map(String, JSON)|Map",
        "Tuple(JSON, UInt64)|Tuple", "LowCardinality(JSON)|LowCardinality", "String|String"
    }, delimiter = '|')
    public void unrelatedTypesKeepExistingCacheLookup(@NotNull String name, @NotNull String cachedName) throws Exception {
        var dataSource = dataSourceWithTypeCache(true);
        assertSame(dataSource.getLocalDataType(cachedName), dataSource.getLocalDataType(name));
        assertNotSame(ClickhouseJSONValueHandler.INSTANCE,
            handler(name, dataSource.getLocalDataType(name).getDataKind()));
    }

    @NotNull
    private ClickhouseDataSource dataSourceWithTypeCache(boolean populated) throws Exception {
        var dataSource = mock(ClickhouseDataSource.class, CALLS_REAL_METHODS);
        var cache = new ClickhouseDataTypeCache(dataSource);
        cache.setCache(populated ? List.of(
            new GenericDataType(dataSource, Types.CLOB, "JSON", null, false, false, 0, 0, 0),
            new GenericDataType(dataSource, Types.NULL, "Nullable", null, false, false, 0, 0, 0),
            new GenericDataType(dataSource, Types.VARCHAR, "String", null, false, false, 0, 0, 0),
            new GenericDataType(dataSource, Types.ARRAY, "Array", null, false, false, 0, 0, 0),
            new GenericDataType(dataSource, Types.ARRAY, "Map", null, false, false, 0, 0, 0),
            new GenericDataType(dataSource, Types.STRUCT, "Tuple", null, false, false, 0, 0, 0),
            new GenericDataType(dataSource, Types.NULL, "LowCardinality", null, false, false, 0, 0, 0)
        ) : List.of());
        // Avoid opening a connection in the constructor while exercising the real cache lookup, not a stub.
        var cacheField = GenericDataSource.class.getDeclaredField("dataTypeCache");
        cacheField.setAccessible(true);
        cacheField.set(dataSource, cache);
        return dataSource;
    }

    @ParameterizedTest
    @ValueSource(strings = {"String", "Nullable(String)", "Dynamic", "Object('json')", "JSONB", "JSONX",
        "JSON)", "Nullable(JSON", "Nullable(JSON))", "Nullable(JSON)Extra", "LowCardinality(String)",
        "Tuple(JSON)", "Map(String, JSON)", "Array(JSON)", "Array(Nullable(JSON))", "LowCardinality(JSON)",
        "Nullable(Nullable(JSON))", "Nullable(Array(JSON))", "JSON(", "JSON(a UInt64", "JSON(a UInt64))",
        "JSON(a UInt64)Extra", "JSON(a UInt64)Extra()", "Nullable(JSON(a UInt64", "Nullable(JSON(a UInt64)))",
        "Nullable(JSON(a UInt64)Extra)", "Nullable(JSON(a UInt64))Extra", "Nullable(JSONX(a UInt64))",
        "JSON(`a UInt64)", "JSON(SKIP REGEXP 'unfinished)", "JSON(\"a UInt64)", "JSON(a Array(UInt64)"})
    public void unrelatedAndMalformedTypesAreNotJson(@NotNull String name) {
        assertNotSame(ClickhouseJSONValueHandler.INSTANCE, handler(name, DBPDataKind.UNKNOWN));
        assertNotSame(ClickhouseJSONValueHandler.INSTANCE, handler(name, DBPDataKind.CONTENT));
        var dataSource = mock(ClickhouseDataSource.class, CALLS_REAL_METHODS);
        doReturn(null).when(dataSource).getLocalDataType(anyString());
        assertNotEquals(DBPDataKind.CONTENT, dataSource.resolveDataKind(name, Types.OTHER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Array(JSON)", "Array(JSON(a UInt64))", "Array(Nullable(JSON))",
        "Array(Array(JSON))", "Array(String)", "Array(UInt64)", "Map(String, JSON)", "Map(UInt64, String)"})
    public void arraysAndMapsKeepCollectionHandler(@NotNull String name) {
        assertSame(ClickhouseArrayValueHandler.INSTANCE, handler(name, DBPDataKind.ARRAY));
        if (name.startsWith("Array(")) {
            var dataSource = mock(ClickhouseDataSource.class, CALLS_REAL_METHODS);
            assertEquals(DBPDataKind.ARRAY, dataSource.resolveDataKind(name, Types.OTHER));
        }
    }

    @Test
    public void tupleKeepsStructHandler() {
        assertSame(ClickhouseStructValueHandler.INSTANCE, handler("Tuple(JSON, UInt64)", DBPDataKind.STRUCT));
        var dataSource = mock(ClickhouseDataSource.class, CALLS_REAL_METHODS);
        assertEquals(DBPDataKind.STRUCT, dataSource.resolveDataKind("Tuple(JSON, UInt64)", Types.OTHER));
    }

    @ParameterizedTest
    @MethodSource("supportedJsonTypes")
    public void fetchingUsesJdbcStringAndPreservesSqlNull(@NotNull String name) throws Exception {
        DBCSession session = mock(DBCSession.class);
        JDBCResultSet resultSet = mock(JDBCResultSet.class);
        when(resultSet.getString(1)).thenReturn("{\"a\":1}", null);
        var value = (ClickhouseContentJSON) ClickhouseJSONValueHandler.INSTANCE.fetchValueObject(
            session, resultSet, type(name, DBPDataKind.CONTENT), 0);
        assertEquals("{\"a\":1}", value.getDisplayString(DBDDisplayFormat.EDIT));
        assertEquals(MimeTypes.TEXT_JSON, value.getContentType());
        var nullValue = (ClickhouseContentJSON) ClickhouseJSONValueHandler.INSTANCE.fetchValueObject(
            session, resultSet, type(name, DBPDataKind.CONTENT), 0);
        assertTrue(nullValue.isNull());
        verify(resultSet, times(2)).getString(1);
        verify(resultSet, never()).getObject(anyInt());
    }

    @Test
    public void fetchFailureIsNotTurnedIntoNullOrEmptyContent() throws Exception {
        DBCSession session = mock(DBCSession.class);
        when(session.getExecutionContext()).thenReturn(mock(DBCExecutionContext.class));
        JDBCResultSet resultSet = mock(JDBCResultSet.class);
        SQLException failure = new SQLException("Synthetic transport failure");
        when(resultSet.getString(1)).thenThrow(failure);
        var exception = assertThrows(DBCException.class, () -> ClickhouseJSONValueHandler.INSTANCE.fetchValueObject(
            session, resultSet, type("Nullable(JSON(a UInt64))", DBPDataKind.CONTENT), 0));
        assertSame(failure, exception.getCause());
    }

    @Test
    public void copyingKeepsJsonMimeAndRawText() {
        var value = new ClickhouseContentJSON(null, "{\"a\":1}");
        var copy = value.cloneValue(monitor);
        assertNotSame(value, copy);
        assertEquals(MimeTypes.TEXT_JSON, copy.getContentType());
        assertEquals(value.getRawValue(), copy.getRawValue());
    }

    @Test
    public void handlerBindingKeepsSqlNullDistinctFromJsonNull() throws Exception {
        JDBCPreparedStatement statement = mock(JDBCPreparedStatement.class);
        JDBCSession session = mock(JDBCSession.class);
        DBSTypedObject type = type("Nullable(JSON)", DBPDataKind.CONTENT);
        ClickhouseJSONValueHandler.INSTANCE.bindValueObject(session, statement, type, 0, new ClickhouseContentJSON(null, null));
        ClickhouseJSONValueHandler.INSTANCE.bindValueObject(session, statement, type, 1, new ClickhouseContentJSON(null, "null"));
        verify(statement).setNull(1, Types.OTHER, "Nullable(JSON)");
        verify(statement).setString(2, "null");
    }

    @Test
    public void extensionRegistrationRemainsClickhouseSpecific() {
        var config = providerConfiguration();
        assertFalse(new ValueHandlerDescriptor(config).isGlobal());
        assertEquals("clickhouse", config.getChildren("datasource")[0].getAttribute("id"));
    }

    @ParameterizedTest
    @MethodSource("supportedJsonTypes")
    public void extensionRegistrationAdmitsScalarJsonVariants(@NotNull String name) {
        var descriptor = new ValueHandlerDescriptor(providerConfiguration());
        assertTrue(descriptor.supportsType(type(name, DBPDataKind.UNKNOWN)));
    }

    @Test
    public void extensionRegistrationDoesNotCaptureUnrelatedScalarTypes() {
        var descriptor = new ValueHandlerDescriptor(providerConfiguration());
        assertFalse(descriptor.supportsType(type("JSONB", DBPDataKind.UNKNOWN)));
        assertFalse(descriptor.supportsType(type("JSONX", DBPDataKind.UNKNOWN)));
        assertFalse(descriptor.supportsType(type("Nullable(JSON)Extra", DBPDataKind.UNKNOWN)));
        assertFalse(descriptor.supportsType(type("Nullable(String)", DBPDataKind.STRING)));
        assertFalse(descriptor.supportsType(type("String", DBPDataKind.STRING)));
    }

    @NotNull
    private IConfigurationElement providerConfiguration() {
        return Arrays.stream(Platform.getExtensionRegistry().getConfigurationElementsFor("org.jkiss.dbeaver.dataTypeProvider"))
            .filter(element -> "ClickhouseValueHandlerProvider".equals(element.getAttribute("id")))
            .findFirst().orElseThrow();
    }
}
