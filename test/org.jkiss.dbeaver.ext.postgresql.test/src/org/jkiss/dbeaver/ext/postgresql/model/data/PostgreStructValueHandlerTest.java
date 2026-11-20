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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.transformers.ComplexTypeAttributeTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCompositeDynamic;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStructValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryType;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.Savepoint;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Map;

public class PostgreStructValueHandlerTest extends DBeaverUnitTest {
    @Mock
    private DBCSession session;
    @Mock
    private PostgreDataSource dataSource;
    @Mock
    private PostgreDataType resolvedType;
    @Mock
    private PostgreDatabase database;
    @Mock
    private DBCExecutionContext executionContext;
    @Mock
    private JDBCColumnMetaData column;
    @Mock
    private DBPDataSourceContainer container;
    @Mock
    private DBPDriver driver;
    @Mock
    private JDBCSession jdbcSession;
    @Mock
    private JDBCPreparedStatement statement;
    @Mock
    private JDBCResultSet resultSet;
    @Mock
    private JDBCPreparedStatement lookupStatement;
    @Mock
    private JDBCPreparedStatement columnsStatement;
    @Mock
    private JDBCResultSet tableResultSet;
    @Mock
    private JDBCResultSet columnsResultSet;
    @Mock
    private PostgreServerExtension serverType;
    @Mock
    private PostgreTableBase view;
    @Mock
    private PostgreTableColumn firstColumn;
    @Mock
    private PostgreTableColumn secondColumn;
    @Mock
    private DBPPreferenceStore preferences;
    @Mock
    private DBDAttributeBindingMeta binding;
    @Mock
    private DBVModel virtualModel;
    @Mock
    private DBCExecutionSource executionSource;
    @Mock
    private SQLQuery sqlQuery;
    @Mock
    private ResultSetMetaData recordMetaData;
    @Mock
    private Savepoint savepoint;

    @BeforeEach
    public void setUp() {
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(container.getDriver()).thenReturn(driver);
        Mockito.when(session.getDefaultValueHandler()).thenReturn(JDBCStringValueHandler.INSTANCE);
    }

    @Test
    public void nonResultTypeDoesNotBecomeDynamicStruct() throws Exception {
        Mockito.when(resolvedType.getTypeID()).thenReturn(Types.VARCHAR);

        String rowValue = "(1,\"AC,DC\",,\"\")";
        Assertions.assertSame(rowValue, PostgreStructValueHandler.INSTANCE.getValueFromObject(
            session, resolvedType, rowValue, false, false));
    }

    @Test
    public void unresolvedTemporaryRowTypeIsLookedUpOnlyOncePerResultColumn() throws Exception {
        Mockito.when(session.getExecutionContext()).thenReturn(executionContext);
        Mockito.when(column.getCatalogName()).thenReturn("postgres");
        Mockito.when(column.getTypeName()).thenReturn("view6");
        Mockito.when(column.getTypeID()).thenReturn(Types.STRUCT);
        Mockito.when(dataSource.getDatabase("postgres")).thenReturn(database);

        String rowValue = "(1,AC/DC)";
        Assertions.assertSame(rowValue, PostgreStructValueHandler.INSTANCE.getValueFromObject(
            session, column, rowValue, false, false));
        Assertions.assertSame(rowValue, PostgreStructValueHandler.INSTANCE.getValueFromObject(
            session, column, rowValue, false, false));
        Mockito.verify(database).getDataType(Mockito.any(), Mockito.eq("view6"), Mockito.eq(false));
    }

    @Test
    public void nonRowValueIsPreserved() throws Exception {
        Mockito.when(resolvedType.getTypeID()).thenReturn(Types.VARCHAR);

        String value = "not a row";
        Assertions.assertSame(value, PostgreStructValueHandler.INSTANCE.getValueFromObject(
            session, resolvedType, value, false, false));
    }

    @Test
    public void temporaryViewRowUsesAttributeNamesFromQuerySession() throws Exception {
        Mockito.when(jdbcSession.getDataSource()).thenReturn(dataSource);
        Mockito.when(jdbcSession.getDefaultValueHandler()).thenReturn(JDBCStringValueHandler.INSTANCE);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(dataSource.getDatabase("postgres")).thenReturn(database);
        Mockito.when(database.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getServerType()).thenReturn(serverType);
        Mockito.when(column.getCatalogName()).thenReturn("postgres");
        Mockito.when(column.getSchemaName()).thenReturn("test2");
        Mockito.when(column.getTypeName()).thenReturn("view6");
        Mockito.when(column.getTypeID()).thenReturn(Types.STRUCT);
        Mockito.when(resolvedType.getTypeID()).thenReturn(Types.VARCHAR);
        Mockito.when(database.getDataType(Mockito.any(), Mockito.eq("view6"), Mockito.eq(false))).thenReturn(resolvedType);
        Mockito.when(jdbcSession.prepareStatement(Mockito.contains("pg_namespace"))).thenReturn(statement);
        Mockito.when(jdbcSession.prepareStatement(Mockito.contains("pg_class c"))).thenReturn(lookupStatement);
        Mockito.when(jdbcSession.prepareStatement(Mockito.contains("pg_attribute a"))).thenReturn(columnsStatement);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenReturn(true);
        Mockito.when(resultSet.getString("nspname")).thenReturn("pg_temp_3");
        Mockito.when(resultSet.getLong("oid")).thenReturn(234L);
        Mockito.when(lookupStatement.getResultSet()).thenReturn(tableResultSet);
        Mockito.when(tableResultSet.next()).thenReturn(true);
        Mockito.when(tableResultSet.getString("relkind")).thenReturn("v");
        Mockito.when(serverType.createRelationOfClass(
            Mockito.any(PostgreSchema.class), Mockito.any(), Mockito.eq(tableResultSet)))
            .thenReturn(view);
        Mockito.when(view.getObjectId()).thenReturn(456L);
        Mockito.when(columnsStatement.getResultSet()).thenReturn(columnsResultSet);
        Mockito.when(columnsResultSet.next()).thenReturn(true, true, false);
        Mockito.when(view.createTableColumn(Mockito.any(), Mockito.any(PostgreSchema.class), Mockito.eq(columnsResultSet)))
            .thenReturn(firstColumn, secondColumn);
        Mockito.when(firstColumn.getOrdinalPosition()).thenReturn(1);
        Mockito.when(firstColumn.getName()).thenReturn("category_id");
        Mockito.when(secondColumn.getOrdinalPosition()).thenReturn(2);
        Mockito.when(secondColumn.getName()).thenReturn("name");

        for (int i = 0; i < 2; i++) {
            JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
                PostgreStructValueHandler.INSTANCE.getValueFromObject(jdbcSession, column, "(1,AC/DC)", false, false));
            Assertions.assertEquals("category_id", composite.getAttributes()[0].getName());
            Assertions.assertEquals("name", composite.getAttributes()[1].getName());
            Assertions.assertEquals("(1,AC/DC)", PostgreStructValueHandler.INSTANCE.getValueDisplayString(
                column, composite, DBDDisplayFormat.NATIVE));
        }
        Mockito.verify(lookupStatement).setString(2, "view6");
        Mockito.verify(columnsStatement).setLong(1, 456L);
        Mockito.verify(jdbcSession).prepareStatement(Mockito.contains("pg_namespace"));
    }

    @Test
    public void unavailableTemporaryViewDoesNotBecomeDynamicStruct() throws Exception {
        Mockito.when(jdbcSession.getDataSource()).thenReturn(dataSource);
        Mockito.when(jdbcSession.getDefaultValueHandler()).thenReturn(JDBCStringValueHandler.INSTANCE);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(dataSource.getDatabase("postgres")).thenReturn(database);
        Mockito.when(database.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getServerType()).thenReturn(serverType);
        Mockito.when(column.getCatalogName()).thenReturn("postgres");
        Mockito.when(column.getTypeName()).thenReturn("view6");
        Mockito.when(column.getTypeID()).thenReturn(Types.STRUCT);
        Mockito.when(resolvedType.getTypeID()).thenReturn(Types.VARCHAR);
        Mockito.when(database.getDataType(Mockito.any(), Mockito.eq("view6"), Mockito.eq(false))).thenReturn(resolvedType);
        Mockito.when(jdbcSession.prepareStatement(Mockito.contains("pg_namespace"))).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);

        String rowValue = "(1,AC/DC)";
        Assertions.assertSame(rowValue, PostgreStructValueHandler.INSTANCE.getValueFromObject(
            jdbcSession, column, rowValue, false, false));
    }

    @Test
    public void qualifiedNonTemporaryTypeDoesNotUseTemporaryView() throws Exception {
        Mockito.when(jdbcSession.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(dataSource.getDatabase("postgres")).thenReturn(database);
        Mockito.when(database.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getServerType()).thenReturn(serverType);
        Mockito.when(column.getCatalogName()).thenReturn("postgres");
        Mockito.when(column.getSchemaName()).thenReturn("test2");
        Mockito.when(column.getTypeName()).thenReturn("public.view6");
        Mockito.when(column.getTypeID()).thenReturn(Types.STRUCT);
        Mockito.when(resolvedType.getTypeID()).thenReturn(Types.VARCHAR);
        Mockito.when(database.getDataType(Mockito.any(), Mockito.eq("view6"), Mockito.eq(false))).thenReturn(resolvedType);
        Mockito.when(jdbcSession.prepareStatement(Mockito.contains("pg_namespace"))).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenReturn(true);
        Mockito.when(resultSet.getString("nspname")).thenReturn("pg_temp_3");
        Mockito.when(resultSet.getLong("oid")).thenReturn(234L);

        String rowValue = "(1,AC/DC)";
        Assertions.assertSame(rowValue, PostgreStructValueHandler.INSTANCE.getValueFromObject(
            jdbcSession, column, rowValue, false, false));
        Mockito.verify(jdbcSession, Mockito.never()).prepareStatement(Mockito.contains("pg_class c"));
    }

    @Test
    public void derivedTableRecordIsDynamicStruct() throws Exception {
        Mockito.when(column.getTypeName()).thenReturn(PostgreConstants.TYPE_RECORD);
        Mockito.when(column.getTypeID()).thenReturn(Types.OTHER);

        PostgreValueHandlerProvider provider = new PostgreValueHandlerProvider();
        Assertions.assertSame(PostgreStructValueHandler.INSTANCE, provider.getValueHandler(dataSource, session, column));

        JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
            PostgreStructValueHandler.INSTANCE.getValueFromObject(session, column, "(1,pg_class)", false, false));
        Assertions.assertArrayEquals(new Object[]{"1", "pg_class"}, composite.getValues());
        Assertions.assertEquals("Attr0", composite.getAttributes()[0].getName());
        Assertions.assertEquals("Attr1", composite.getAttributes()[1].getName());
        Assertions.assertEquals("(1,pg_class)", composite.getStringRepresentation());
        Assertions.assertEquals("(1,pg_class)", JDBCStructValueHandler.INSTANCE.getValueDisplayString(
            column, composite, DBDDisplayFormat.UI));
        Mockito.when(session.getProgressMonitor()).thenReturn(new VoidProgressMonitor());
        JDBCCompositeDynamic copy = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
            PostgreStructValueHandler.INSTANCE.getValueFromObject(session, column, composite, true, false));
        Assertions.assertNotSame(composite, copy);
        Assertions.assertEquals("(1,pg_class)", copy.getStringRepresentation());
        Mockito.verifyNoInteractions(database);
    }

    @Test
    public void recordDataKindIsStruct() {
        Mockito.doCallRealMethod().when(dataSource).resolveDataKind(PostgreConstants.TYPE_RECORD, Types.OTHER);

        Assertions.assertEquals(DBPDataKind.STRUCT,
            dataSource.resolveDataKind(PostgreConstants.TYPE_RECORD, Types.OTHER));
    }

    @Test
    public void derivedRecordUsesDescribedSelectMemberNames() throws Exception {
        mockRecordQuery("x", "select x from (select * from pg_class) x;", "oid", "relname");
        Mockito.when(recordMetaData.getColumnType(1)).thenReturn(Types.BIGINT);
        Mockito.when(recordMetaData.getColumnTypeName(1)).thenReturn("oid");

        for (int i = 0; i < 2; i++) {
            JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
                PostgreStructValueHandler.INSTANCE.getValueFromObject(jdbcSession, column, "(1,pg_class)", false, false));
            Assertions.assertEquals("oid", composite.getAttributes()[0].getName());
            Assertions.assertEquals("relname", composite.getAttributes()[1].getName());
            Assertions.assertEquals(Types.BIGINT, composite.getAttributes()[0].getTypeID());
            Assertions.assertEquals("oid", composite.getAttributes()[0].getTypeName());
            Assertions.assertArrayEquals(new Object[]{1L, "pg_class"}, composite.getValues());
        }
        Mockito.verify(jdbcSession).prepareStatement(Mockito.contains("FROM (select x from (select * from pg_class) x)"));
        Mockito.verify(statement, Mockito.never()).executeQuery();
        Mockito.verify(jdbcSession, Mockito.never()).setSavepoint();
    }

    @Test
    public void rowConstructorUsesPostgreSQLRecordMemberNames() throws Exception {
        mockRecordQuery("main", "select (id,name) as main, username from test;", "f1", "f2");

        JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
            PostgreStructValueHandler.INSTANCE.getValueFromObject(jdbcSession, column, "(1,john)", false, false));
        Assertions.assertEquals("f1", composite.getAttributes()[0].getName());
        Assertions.assertEquals("f2", composite.getAttributes()[1].getName());
        Assertions.assertEquals(Types.INTEGER, composite.getAttributes()[0].getTypeID());
        Assertions.assertEquals("int4", composite.getAttributes()[0].getTypeName());
        Assertions.assertArrayEquals(new Object[]{1, "john"}, composite.getValues());
        Mockito.verify(statement, Mockito.never()).executeQuery();
    }

    @Test
    public void numericRecordMemberRetainsExactPrecision() throws Exception {
        mockRecordQuery("main", "select (amount,name) as main from test", "amount", "name");
        Mockito.when(recordMetaData.getColumnType(1)).thenReturn(Types.NUMERIC);
        Mockito.when(recordMetaData.getColumnTypeName(1)).thenReturn("numeric");

        String numeric = "12345678901234567890.123456789";
        JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
            PostgreStructValueHandler.INSTANCE.getValueFromObject(jdbcSession, column, "(" + numeric + ",john)", false, false));
        Assertions.assertEquals(Types.NUMERIC, composite.getAttributes()[0].getTypeID());
        Assertions.assertEquals(new BigDecimal(numeric), composite.getValues()[0]);
    }

    @Test
    public void unconvertibleRecordMemberKeepsOriginalValueAndDeclaredType() throws Exception {
        mockRecordQuery("main", "select (ids,name) as main from test", "ids", "name");
        Mockito.when(recordMetaData.getColumnType(1)).thenReturn(Types.ARRAY);
        Mockito.when(recordMetaData.getColumnTypeName(1)).thenReturn("_int4");

        JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
            PostgreStructValueHandler.INSTANCE.getValueFromObject(jdbcSession, column, "(\"{1,2}\",john)", false, false));
        Assertions.assertEquals(Types.ARRAY, composite.getAttributes()[0].getTypeID());
        Assertions.assertEquals("_int4", composite.getAttributes()[0].getTypeName());
        Assertions.assertEquals("{1,2}", composite.getValues()[0]);
    }

    @Test
    public void recordKeepsSyntheticNamesWhenSelectCannotBeDescribed() throws Exception {
        mockRecordQuery("x", "select x from (select * from pg_class) x", "oid", "relname");
        Mockito.when(statement.getMetaData()).thenThrow(new SQLException("Cannot describe record"));

        JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
            PostgreStructValueHandler.INSTANCE.getValueFromObject(jdbcSession, column, "(1,pg_class)", false, false));
        Assertions.assertEquals("Attr0", composite.getAttributes()[0].getName());
        Assertions.assertEquals("(1,pg_class)", composite.getStringRepresentation());
    }

    @Test
    public void failedRecordDescribeRestoresManualTransaction() throws Exception {
        mockRecordQuery("x", "select ROW(1,2) as x, ROW(3,4) as x", "f1", "f2");
        Mockito.when(jdbcSession.getAutoCommit()).thenReturn(false);
        Mockito.when(jdbcSession.setSavepoint()).thenReturn(savepoint);
        Mockito.when(statement.getMetaData()).thenThrow(new SQLException("column x is ambiguous"));

        JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
            PostgreStructValueHandler.INSTANCE.getValueFromObject(jdbcSession, column, "(1,2)", false, false));
        Assertions.assertEquals("Attr0", composite.getAttributes()[0].getName());
        var calls = Mockito.inOrder(jdbcSession, statement);
        calls.verify(jdbcSession).setSavepoint();
        calls.verify(jdbcSession).prepareStatement(Mockito.anyString());
        calls.verify(statement).getMetaData();
        calls.verify(jdbcSession).rollback(savepoint);
        calls.verify(jdbcSession).releaseSavepoint(savepoint);
    }

    @Test
    public void successfulRecordDescribeReleasesSavepointWithoutRollback() throws Exception {
        mockRecordQuery("main", "select (id,name) as main from test", "f1", "f2");
        Mockito.when(jdbcSession.getAutoCommit()).thenReturn(false);
        Mockito.when(jdbcSession.setSavepoint()).thenReturn(savepoint);

        JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
            PostgreStructValueHandler.INSTANCE.getValueFromObject(jdbcSession, column, "(1,john)", false, false));
        Assertions.assertEquals("f1", composite.getAttributes()[0].getName());
        Mockito.verify(jdbcSession).releaseSavepoint(savepoint);
        Mockito.verify(jdbcSession, Mockito.never()).rollback(savepoint);
    }

    @Test
    public void unsupportedSavepointSkipsRecordDescribe() throws Exception {
        mockRecordQuery("main", "select (id,name) as main from test", "f1", "f2");
        Mockito.when(jdbcSession.getAutoCommit()).thenReturn(false);
        Mockito.when(jdbcSession.setSavepoint()).thenThrow(new SQLException("Savepoints not supported"));

        JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
            PostgreStructValueHandler.INSTANCE.getValueFromObject(jdbcSession, column, "(1,john)", false, false));
        Assertions.assertEquals("Attr0", composite.getAttributes()[0].getName());
        Mockito.verify(jdbcSession, Mockito.never()).prepareStatement(Mockito.anyString());
    }

    private void mockRecordQuery(
        @NotNull String columnName,
        @NotNull String sql,
        @NotNull String firstName,
        @NotNull String secondName
    ) throws Exception {
        Mockito.when(jdbcSession.getDataSource()).thenReturn(dataSource);
        Mockito.when(jdbcSession.getAutoCommit()).thenReturn(true);
        Mockito.when(jdbcSession.getDefaultValueHandler()).thenReturn(JDBCStringValueHandler.INSTANCE);
        Mockito.when(dataSource.resolveDataKind(Mockito.anyString(), Mockito.anyInt()))
            .thenAnswer(invocation -> JDBCUtils.getDataKindByTypeID(invocation.getArgument(1), invocation.getArgument(0)));
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(column.getTypeName()).thenReturn(PostgreConstants.TYPE_RECORD);
        Mockito.when(column.getLabel()).thenReturn(columnName);
        Mockito.when(column.getSource()).thenReturn(executionSource);
        Mockito.when(executionSource.getSourceDescriptor()).thenReturn(sqlQuery);
        Mockito.when(sqlQuery.getType()).thenReturn(SQLQueryType.SELECT);
        Mockito.when(sqlQuery.getText()).thenReturn(sql);
        Mockito.when(jdbcSession.prepareStatement(Mockito.anyString())).thenReturn(statement);
        Mockito.when(statement.getMetaData()).thenReturn(recordMetaData);
        Mockito.when(recordMetaData.getColumnCount()).thenReturn(2);
        Mockito.when(recordMetaData.getColumnName(1)).thenReturn(firstName);
        Mockito.when(recordMetaData.getColumnName(2)).thenReturn(secondName);
        Mockito.when(recordMetaData.getColumnType(1)).thenReturn(Types.INTEGER);
        Mockito.when(recordMetaData.getColumnTypeName(1)).thenReturn("int4");
        Mockito.when(recordMetaData.getColumnType(2)).thenReturn(Types.VARCHAR);
        Mockito.when(recordMetaData.getColumnTypeName(2)).thenReturn("text");
    }

    @Test
    public void anonymousRecordBindsNestedAttributesFromFetchedValue() throws Exception {
        Mockito.when(column.getTypeName()).thenReturn(PostgreConstants.TYPE_RECORD);
        final JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
            PostgreStructValueHandler.INSTANCE.getValueFromObject(session, column, "(1,pg_class)", false, false));
        Mockito.when(container.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(container.getVirtualModel()).thenReturn(virtualModel);
        Mockito.when(preferences.getBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES)).thenReturn(true);
        Mockito.when(binding.getDataType()).thenReturn(resolvedType);
        Mockito.when(binding.getDataSource()).thenReturn(dataSource);
        Mockito.when(binding.getOrdinalPosition()).thenReturn(0);

        new ComplexTypeAttributeTransformer().transformAttribute(
            session, binding, Collections.singletonList(new Object[]{composite}), Map.of());

        Mockito.verify(binding).setNestedBindings(Mockito.argThat(nested ->
            nested.size() == 2 && "Attr0".equals(nested.get(0).getName()) && "Attr1".equals(nested.get(1).getName())));
    }

}
