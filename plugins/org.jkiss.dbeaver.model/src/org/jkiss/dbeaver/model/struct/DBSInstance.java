/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

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
     * @param meta request for metadata operations context
     * @return default data source execution context.
     */
    @NotNull
    DBCExecutionContext getDefaultContext(boolean meta);

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
     * @return execution context
     */
    @NotNull
    DBCExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBException;

    void shutdown(DBRProgressMonitor monitor);
}
