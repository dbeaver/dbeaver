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
package org.jkiss.dbeaver.model.dpi;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.app.DBPPingController;
import org.jkiss.utils.rest.RequestMapping;
import org.jkiss.utils.rest.RequestParameter;

/**
 * Detached data source proxy.
 */
public interface DPIController extends DBPPingController, AutoCloseable {

    @RequestMapping
    String ping() throws DBException;

    /**
     * Opens new session
     */
    @RequestMapping
    DPISession openSession() throws DBException;

    @RequestMapping
    @NotNull
    DBPDataSource openDataSource(
        @RequestParameter("parameters") @NotNull DPIDataSourceParameters parameters
    ) throws DBException;

    @RequestMapping
    // Closes session and terminates detached process when last session is closed
    void closeSession(@RequestParameter("session") @NotNull String sessionId) throws DBException;

    @RequestMapping
    Object callMethod(
        @RequestParameter("object") @NotNull String objectId,
        @RequestParameter("method") @NotNull String method,
        @RequestParameter("args") @Nullable Object[] args) throws DBException;

    @RequestMapping
    Object readProperty(
        @RequestParameter("object") @NotNull String objectId,
        @RequestParameter("property") @NotNull String propertyName) throws DBException;

}
