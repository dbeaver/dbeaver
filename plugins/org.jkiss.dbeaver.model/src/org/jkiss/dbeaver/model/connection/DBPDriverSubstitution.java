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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.net.DBWHandlerDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Driver;
import java.util.Properties;

public interface DBPDriverSubstitution {
    @NotNull
    Driver getSubstitutingDriverInstance(@NotNull DBRProgressMonitor monitor);

    @Nullable
    String getConnectionURL(
        @NotNull DBPDataSourceContainer container,
        @NotNull DBPConnectionConfiguration configuration);

    @Nullable
    Properties getConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull DBPConnectionConfiguration configuration);

    boolean isNetworkHandlerSupported(@NotNull DBWHandlerDescriptor descriptor);

    boolean isAuthModelSupported(@NotNull DBPAuthModelDescriptor descriptor);
}
