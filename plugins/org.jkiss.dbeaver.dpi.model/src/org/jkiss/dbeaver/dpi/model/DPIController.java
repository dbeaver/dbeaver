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
package org.jkiss.dbeaver.dpi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.utils.rest.RequestMapping;
import org.jkiss.utils.rest.RequestParameter;

import java.util.Map;

/**
 * Detached data source proxy.
 */
public interface DPIController extends AutoCloseable {

    @RequestMapping
    String ping() throws DBException;

    /**
     * Opens new session
     */
    @RequestMapping
    DPISession openSession(@RequestParameter("project") String projectId) throws DBException;

    @RequestMapping
    @NotNull
    DBPDataSource openDataSource(
        @RequestParameter("session") @NotNull String session,
        @RequestParameter("project") String projectId,
        @RequestParameter("container") @NotNull String container,
        @RequestParameter("credentials") @Nullable Map<String, String> credentials)
        throws DBException;

    @RequestMapping
    // Closes session and terminates detached process when last session is closed
    void closeSession(@RequestParameter("session") @NotNull String sessionId) throws DBException;

    @RequestMapping
    Object callMethod(
        @RequestParameter("object") @NotNull String objectId,
        @RequestParameter("method") @NotNull String method,
        @RequestParameter("args") @Nullable Object[] args) throws DBException;
}
