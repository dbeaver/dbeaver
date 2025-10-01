/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * Completion engine
 */
public interface AIEngine extends AutoCloseable {

    @NotNull
    List<AIModel> getModels(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Requests completions from the completion engine.
     *
     * @param monitor the progress monitor
     * @param request the completion request
     * @return the completion response
     * @throws TooManyRequestsException if the request limit is exceeded and the request can be retried
     * @throws DBException if an error occurs
     */
    @NotNull
    AIEngineResponse requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException;

    /**
     * Requests a stream of completion chunks from the completion engine.
     *
     * @param monitor  the progress monitor
     * @param request  the completion request
     * @param listener
     * @throws TooManyRequestsException if the request limit is exceeded and the request can be retried
     * @throws DBException              if an error occurs
     */
    void requestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException;

    int getContextWindowSize(DBRProgressMonitor monitor) throws DBException;

    @Override
    void close() throws DBException;
}
