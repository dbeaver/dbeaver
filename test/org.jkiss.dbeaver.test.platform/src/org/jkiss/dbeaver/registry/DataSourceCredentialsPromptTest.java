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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.bigquery.auth.BQAuthModel;
import org.jkiss.dbeaver.ext.mssql.auth.SQLServerAuthModelADIntegrated;
import org.jkiss.dbeaver.ext.mssql.auth.SQLServerAuthModelADPassword;
import org.jkiss.dbeaver.ext.mssql.auth.SQLServerAuthModelCustom;
import org.jkiss.dbeaver.ext.mssql.auth.SQLServerAuthModelDatabase;
import org.jkiss.dbeaver.ext.mssql.auth.SQLServerAuthModelMFA;
import org.jkiss.dbeaver.ext.mssql.auth.SQLServerAuthModelMSI;
import org.jkiss.dbeaver.ext.mssql.auth.SQLServerAuthModelNTLM;
import org.jkiss.dbeaver.ext.mssql.auth.SQLServerAuthModelWindows;
import org.jkiss.dbeaver.ext.oracle.model.auth.OracleAuthOS;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.stream.Stream;

public class DataSourceCredentialsPromptTest {
    @ParameterizedTest
    @MethodSource("integratedAuthModels")
    public void testIntegratedAuthenticationSkipsCredentialsDialog(@NotNull DBAAuthModel<?> authModel) {
        DBPConnectionConfiguration configuration = Mockito.mock(DBPConnectionConfiguration.class);
        Mockito.doReturn(authModel).when(configuration).getAuthModel();
        DataSourceDescriptor dataSource = Mockito.mock(DataSourceDescriptor.class);
        Mockito.when(dataSource.getActualConnectionConfiguration()).thenReturn(configuration);
        Mockito.when(dataSource.getConnectionConfiguration()).thenReturn(configuration);

        Assertions.assertFalse(authModel.isCredentialsPromptRequired());
        Assertions.assertTrue(DataSourceDescriptor.askForPassword(dataSource, null, DBWTunnel.AuthCredentials.CREDENTIALS));
        Assertions.assertTrue(DataSourceDescriptor.askForPassword(dataSource, null, DBWTunnel.AuthCredentials.CREDENTIALS));

        Mockito.verify(dataSource, Mockito.times(2)).getActualConnectionConfiguration();
        Mockito.verify(dataSource, Mockito.times(2)).getConnectionConfiguration();
        Mockito.verifyNoMoreInteractions(dataSource);
        Mockito.verify(configuration, Mockito.times(2)).getAuthModel();
        Mockito.verifyNoMoreInteractions(configuration);
    }

    @ParameterizedTest
    @MethodSource("promptAuthModels")
    public void testOtherAuthenticationStillRequiresCredentialsDialog(@NotNull DBAAuthModel<?> authModel) {
        Assertions.assertTrue(authModel.isCredentialsPromptRequired());
    }

    @NotNull
    private static Stream<DBAAuthModel<?>> integratedAuthModels() {
        return Stream.of(new SQLServerAuthModelADIntegrated(), new SQLServerAuthModelWindows(), new OracleAuthOS());
    }

    @NotNull
    private static Stream<DBAAuthModel<?>> promptAuthModels() {
        return Stream.of(
            new AuthModelDatabaseNative<>(),
            new SQLServerAuthModelDatabase(),
            new SQLServerAuthModelADPassword(),
            new SQLServerAuthModelNTLM(),
            new SQLServerAuthModelMFA(),
            new SQLServerAuthModelMSI(),
            new SQLServerAuthModelCustom(),
            new BQAuthModel()
        );
    }
}
