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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

public class OracleMetadataOptimizerTest extends DBeaverUnitTest {

    private OracleDataSource dataSource;
    private OracleExecutionContext context;
    private DBPConnectionConfiguration configuration;
    private DBPPreferenceStore preferences;
    private JDBCDatabaseMetaData metadata;
    private JDBCSession session;

    @BeforeEach
    public void setUp() throws Exception {
        dataSource = Mockito.mock(OracleDataSource.class, Mockito.CALLS_REAL_METHODS);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.doReturn(container).when(dataSource).getContainer();
        configuration = new DBPConnectionConfiguration();
        Mockito.when(container.getConnectionConfiguration()).thenReturn(configuration);
        preferences = Mockito.mock(DBPPreferenceStore.class);
        Mockito.when(container.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(preferences.getString(ModelPreferences.META_SEPARATE_CONNECTION))
            .thenReturn(ModelPreferences.SeparateConnectionBehavior.ALWAYS.name());
        Mockito.when(preferences.getBoolean(OracleConstants.PROP_USE_META_OPTIMIZER)).thenReturn(true);
        context = Mockito.mock(OracleExecutionContext.class);
        Mockito.when(context.getContextName()).thenReturn(JDBCExecutionContext.TYPE_METADATA);
        session = Mockito.mock(JDBCSession.class);
        Mockito.when(context.openSession(Mockito.eq(monitor), Mockito.any(DBCExecutionPurpose.class), Mockito.anyString()))
            .thenReturn(session);
        metadata = Mockito.mock(JDBCDatabaseMetaData.class);
        Mockito.when(session.getMetaData()).thenReturn(metadata);
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(Mockito.mock(JDBCResultSet.class));
    }

    @ParameterizedTest
    @CsvSource({
        "23, 0, 23.1.0",
        "23, 5, 23.1.0",
        "26, 0, 23.1.0",
        "19, 0, 10.2.0.5",
        "12, 1, 10.2.0.5",
        "10, 2, 10.2.0.5"
    })
    public void configuresCompatibleAutomaticVersion(int major, int minor, @NotNull String expectedVersion) throws Exception {
        Mockito.when(metadata.getDatabaseMajorVersion()).thenReturn(major);
        Mockito.when(metadata.getDatabaseMinorVersion()).thenReturn(minor);

        List<String> statements = initializeContext();

        Assertions.assertTrue(statements.stream().anyMatch(sql ->
            sql.startsWith("ALTER SESSION SET ") && sql.contains("OPTIMIZER_FEATURES_ENABLE='" + expectedVersion + "'")));
        Assertions.assertNull(configuration.getProviderProperty(OracleConstants.PROP_USE_META_OPTIMIZER_VERSION));
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.2.0.5", "19.1.0", "23.1.0"})
    public void preservesExplicitVersion(@NotNull String version) throws Exception {
        Mockito.when(metadata.getDatabaseMajorVersion()).thenReturn(23);
        configuration.setProviderProperty(OracleConstants.PROP_USE_META_OPTIMIZER_VERSION, version);

        Assertions.assertTrue(initializeContext().stream().anyMatch(sql ->
            sql.contains("OPTIMIZER_FEATURES_ENABLE='" + version + "'")));
    }

    @Test
    public void doesNotSetOptimizerVersionOnLegacyServer() throws Exception {
        Mockito.when(metadata.getDatabaseMajorVersion()).thenReturn(9);
        Mockito.when(metadata.getDatabaseMinorVersion()).thenReturn(2);

        Assertions.assertTrue(initializeContext().stream().noneMatch(sql -> sql.contains("OPTIMIZER_FEATURES_ENABLE")));
    }

    @Test
    public void respectsDisabledMetadataOptimizer() throws Exception {
        Mockito.when(metadata.getDatabaseMajorVersion()).thenReturn(23);
        Mockito.when(preferences.getBoolean(OracleConstants.PROP_USE_META_OPTIMIZER)).thenReturn(false);

        Assertions.assertTrue(initializeContext().stream().noneMatch(sql -> sql.contains("OPTIMIZER_FEATURES_ENABLE")));
    }

    private List<String> initializeContext() throws Exception {
        dataSource.initializeContextState(monitor, context, null);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session, Mockito.atLeastOnce()).prepareStatement(sql.capture());
        return sql.getAllValues();
    }
}
