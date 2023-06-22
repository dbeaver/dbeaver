/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.dpi.api;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * Detached data source proxy.
 */
public interface DPIController extends AutoCloseable {

    @ApiEndpoint
    String ping() throws DBException;

    /**
     * Opens new session
     */
    @ApiEndpoint
    DPISession openSession(@ApiParameter("project") String projectId) throws DBException;

    @ApiEndpoint
    @NotNull
    DBPDataSource openDataSource(
        @ApiParameter("session") @NotNull String session,
        @ApiParameter("container") @NotNull String container)
        throws DBException;

    @ApiEndpoint
    // Closes session and terminates detached process when last session is closed
    void closeSession(@ApiParameter("session") @NotNull String sessionId) throws DBException;

}
