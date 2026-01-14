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
package org.jkiss.dbeaver.model.net;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCInvalidatePhase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;

/**
 * Network handler
 */
public interface DBWNetworkHandler {

    @NotNull
    DBPConnectionConfiguration initializeHandler(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull DBPConnectionConfiguration connectionInfo)
        throws DBException, IOException;

    /**
     * Invalidates the network handler in a span of several phases.
     *
     * @param monitor progress monitor
     * @param dataSource data source
     * @param phase invalidation phase
     * @throws DBException on any error to signal the invalidation was not successful
     */
    void invalidateHandler(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull DBCInvalidatePhase phase
    ) throws DBException;

    /**
     * Returns an array of data sources that depend on this handler. The returned array may be empty.
     */
    @NotNull
    default DBPDataSourceContainer[] getDependentDataSources() {
        return new DBPDataSourceContainer[0];
    }
}
