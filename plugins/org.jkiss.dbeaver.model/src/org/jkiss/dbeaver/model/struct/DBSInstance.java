/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPExclusiveResource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Data Source instance.
 * Instance wraps physical connection to database server.
 * Instance manages execution contexts.
 *
 * Single datasource may implement DBSInstance or DBSInstanceContainer
 */
public interface DBSInstance extends DBSObject
{

    /**
     * Default execution context
     *
     * @param monitor progress monitor
     * @param meta request for metadata operations context
     * @return default data source execution context.
     */
    @NotNull
    DBCExecutionContext getDefaultContext(DBRProgressMonitor monitor, boolean meta);

    /**
     * All opened execution contexts
     * @return collection of contexts
     */
    @NotNull
    DBCExecutionContext[] getAllContexts();

    /**
     * Opens new isolated execution context.
     *
     * @param monitor progress monitor
     * @param purpose context purpose (just a descriptive string)
     * @param initFrom initialize new context parameters from specified context
     * @return execution context
     */
    @NotNull
    DBCExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose, @Nullable DBCExecutionContext initFrom) throws DBException;

    void shutdown(DBRProgressMonitor monitor);

    @NotNull
    DBPExclusiveResource getExclusiveLock();

}
