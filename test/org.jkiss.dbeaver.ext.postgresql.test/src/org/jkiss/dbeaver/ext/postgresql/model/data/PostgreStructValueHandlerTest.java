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

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
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
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.transformers.ComplexTypeAttributeTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCompositeDynamic;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStructValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

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
        Mockito.when(serverType.createRelationOfClass(Mockito.any(PostgreSchema.class), Mockito.any(), Mockito.eq(tableResultSet))).thenReturn(view);
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
    public void anonymousRecordBindsNestedAttributesFromFetchedValue() throws Exception {
        Mockito.when(column.getTypeName()).thenReturn(PostgreConstants.TYPE_RECORD);
        JDBCCompositeDynamic composite = Assertions.assertInstanceOf(JDBCCompositeDynamic.class,
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
