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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.connection.DBPDriver;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Authentication result which opens the JDBC connection itself.
 * <p>
 * An auth model may return an implementation of this interface from
 * {@link org.jkiss.dbeaver.model.access.DBAAuthModel#initAuthentication} when the authentication
 * mechanism cannot be expressed through {@link Driver#connect(String, Properties)}. A typical case is
 * a driver-specific connection builder which accepts an object credential, for example a GSS credential
 * used for Kerberos constrained delegation.
 * <p>
 * The method is invoked on the connection opening thread with the context class loader set to the driver
 * class loader, exactly like the default {@link Driver#connect(String, Properties)} call.
 */
public interface JDBCAuthConnectionOpener {

    @NotNull
    Connection openConnection(
        @NotNull DBPDriver driver,
        @Nullable Driver driverInstance,
        @NotNull String url,
        @NotNull Properties connectProps
    ) throws SQLException;

}
