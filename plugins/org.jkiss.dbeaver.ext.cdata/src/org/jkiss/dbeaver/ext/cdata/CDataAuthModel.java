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
package org.jkiss.dbeaver.ext.cdata;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNativeCredentials;

import java.util.Properties;

public class CDataAuthModel extends AuthModelDatabaseNative<AuthModelDatabaseNativeCredentials> {
    public static final String ID = "cdata_native_url_builder";
    public static final String SECRET_PROPERTY_PREFIX = "cdata.secret.";

    @Override
    public void collectConnectionProperties(
        @NotNull DBPDataSourceContainer dataSourceContainer,
        @NotNull AuthModelDatabaseNativeCredentials credentials,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull Properties connectProps,
        boolean collectSecuredProps
    ) {
        super.collectConnectionProperties(dataSourceContainer, credentials, configuration, connectProps, collectSecuredProps);
        if (collectSecuredProps) {
            configuration.getAuthProperties().forEach((name, value) -> {
                if (name.startsWith(SECRET_PROPERTY_PREFIX)) {
                    String propertyName = name.substring(SECRET_PROPERTY_PREFIX.length());
                    connectProps.keySet().removeIf(key -> propertyName.equalsIgnoreCase(key.toString()));
                    connectProps.setProperty(propertyName, value);
                }
            });
        }
    }

    public static void clearSecrets(@NotNull DBPConnectionConfiguration configuration) {
        configuration.getAuthProperties().keySet().removeIf(name -> name.startsWith(SECRET_PROPERTY_PREFIX));
    }
}
