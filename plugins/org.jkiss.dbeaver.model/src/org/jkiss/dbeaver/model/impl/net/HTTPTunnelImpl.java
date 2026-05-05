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
package org.jkiss.dbeaver.model.impl.net;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCInvalidatePhase;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;

/**
 * HTTP(S) tunnel
 */
public class HTTPTunnelImpl implements DBWTunnel {

    @NotNull
    @Override
    public AuthCredentials getRequiredCredentials(@NotNull DBWHandlerConfiguration configuration) {
        return AuthCredentials.NONE;
    }

    @NotNull
    @Override
    public DBPConnectionConfiguration initializeHandler(@NotNull DBRProgressMonitor monitor, @NotNull DBWHandlerConfiguration configuration, @NotNull DBPConnectionConfiguration connectionInfo)
        throws DBException, IOException
    {
        return connectionInfo;
    }

    @Override
    public void closeTunnel(@NotNull DBRProgressMonitor monitor) throws DBException, IOException
    {
    }

    @Nullable
    @Override
    public Object getImplementation() {
        return null;
    }

    @Override
    public void addCloseListener(@NotNull Runnable listener) {
        // do nothing
    }

    @Override
    public boolean matchesParameters(@NotNull String host, int port) {
        return false;
    }

    @Override
    public void invalidateHandler(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull DBCInvalidatePhase phase
    ) throws DBException {
        // nothing to do
    }
}
