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

import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.clickhouse.model.data.ClickhouseContentJSON;
import org.jkiss.dbeaver.ext.clickhouse.model.data.ClickhouseJSONValueHandler;
import org.jkiss.dbeaver.ext.clickhouse.model.data.ClickhouseValueHandlerProvider;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.registry.datatype.ValueHandlerDescriptor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ClickhouseJSONRoutingTest extends DBeaverUnitTest {
    private final ClickhouseValueHandlerProvider provider = new ClickhouseValueHandlerProvider();

    @NotNull
    private static DBSTypedObject type(@NotNull String name) {
        DBSTypedObject type = mock(DBSTypedObject.class);
        when(type.getTypeName()).thenReturn(name);
        when(type.getDataKind()).thenReturn(DBPDataKind.CONTENT);
        return type;
    }

    @NotNull
    private static ValueHandlerDescriptor descriptor() {
        var config = Arrays.stream(Platform.getExtensionRegistry().getConfigurationElementsFor("org.jkiss.dbeaver.dataTypeProvider"))
            .filter(element -> "ClickhouseValueHandlerProvider".equals(element.getAttribute("id")))
            .findFirst().orElseThrow();
        return new ValueHandlerDescriptor(config);
    }

    @ParameterizedTest
    @ValueSource(strings = {"JSON", "JSON()", "JSON(a UInt64, items Array(Nullable(UInt64)))",
        "Nullable(JSON)", "Nullable(JSON())", "Nullable(JSON(a UInt64, items Array(Nullable(UInt64))))",
        "Nullable(JSON(a UInt64)"})
    public void scalarJsonReachesJsonHandler(@NotNull String name) {
        assertTrue(descriptor().supportsType(type(name)), name);
        assertSame(ClickhouseJSONValueHandler.INSTANCE,
            provider.getValueHandler(mock(ClickhouseDataSource.class), mock(DBDFormatSettings.class), type(name)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"JSONB", "Nullable(String)", "Array(JSON)", "LowCardinality(JSON)"})
    public void unrelatedTypesDoNotUseJsonHandler(@NotNull String name) {
        assertNotSame(ClickhouseJSONValueHandler.INSTANCE,
            provider.getValueHandler(mock(ClickhouseDataSource.class), mock(DBDFormatSettings.class), type(name)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"JSON()", "JSON(a UInt64)", "Nullable(JSON)", "Nullable(JSON(a UInt64))"})
    public void populatedCacheSuppliesJsonKindToResultMetadata(@NotNull String name) throws Exception {
        var dataSource = dataSourceWithJsonCache();
        var metadata = mock(ResultSetMetaData.class);
        when(metadata.getColumnTypeName(1)).thenReturn(name);
        when(metadata.getColumnType(1)).thenReturn(Types.OTHER);
        when(metadata.isNullable(1)).thenReturn(ResultSetMetaData.columnNullable);

        var column = new JDBCColumnMetaData(dataSource, metadata, 0);
        assertEquals(name, column.getTypeName());
        assertEquals(DBPDataKind.CONTENT, column.getDataKind());
        assertEquals(Types.CLOB, column.getTypeID());
        assertFalse(column.isRequired());
        assertSame(ClickhouseJSONValueHandler.INSTANCE,
            provider.getValueHandler(dataSource, mock(DBDFormatSettings.class), column));
    }

    @Test
    public void missingCacheFallsBackToJsonContentKind() throws Exception {
        var dataSource = mock(ClickhouseDataSource.class, CALLS_REAL_METHODS);
        doReturn(null).when(dataSource).getLocalDataType(anyString());
        var metadata = mock(ResultSetMetaData.class);
        when(metadata.getColumnTypeName(1)).thenReturn("Nullable(JSON(a UInt64))");
        when(metadata.getColumnType(1)).thenReturn(Types.OTHER);
        assertEquals(DBPDataKind.CONTENT, new JDBCColumnMetaData(dataSource, metadata, 0).getDataKind());
    }

    @Test
    public void jsonContentDistinguishesSqlNullFromJsonNull() throws Exception {
        var session = mock(DBCSession.class);
        var resultSet = mock(JDBCResultSet.class);
        when(resultSet.getString(1)).thenReturn("{\"a\":1}", null, "null");
        var document = (ClickhouseContentJSON) ClickhouseJSONValueHandler.INSTANCE.fetchValueObject(
            session, resultSet, type("Nullable(JSON)"), 0);
        var sqlNull = (ClickhouseContentJSON) ClickhouseJSONValueHandler.INSTANCE.fetchValueObject(
            session, resultSet, type("Nullable(JSON)"), 0);
        var jsonNull = (ClickhouseContentJSON) ClickhouseJSONValueHandler.INSTANCE.fetchValueObject(
            session, resultSet, type("Nullable(JSON)"), 0);
        assertFalse(document.isNull());
        assertTrue(sqlNull.isNull());
        assertFalse(jsonNull.isNull());
        verify(resultSet, times(3)).getString(1);
    }

    @NotNull
    private ClickhouseDataSource dataSourceWithJsonCache() throws Exception {
        when(monitor.isForceCacheUsage()).thenReturn(true);
        var dataSource = new ClickhouseDataSource(monitor, configureTestContainer("com_clickhouse"), new ClickhouseMetaModel()) {
            @Override
            protected void initializeRemoteInstance(@NotNull DBRProgressMonitor monitor) {
                // Populate only the local cache; no ClickHouse connection is needed.
            }

            @NotNull
            @Override
            protected ClickhouseDataTypeCache getDataTypeCache() {
                return (ClickhouseDataTypeCache) super.getDataTypeCache();
            }
        };
        dataSource.getDataTypeCache().setCache(List.of(
            new GenericDataType(dataSource, Types.CLOB, "JSON", null, false, false, 0, 0, 0),
            new GenericDataType(dataSource, Types.NULL, "Nullable", null, false, false, 0, 0, 0)
        ));
        return dataSource;
    }
}
