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
package org.jkiss.dbeaver.model.cli;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CommandLineContext implements AutoCloseable {
    private static final Log log = Log.getLog(CommandLineContext.class);
    @NotNull
    private final Map<String, Object> contextParameter = new LinkedHashMap<>();
    private final List<Runnable> closeHandlers = new ArrayList<>();
    @NotNull
    private final List<Object> results = new ArrayList<>();
    @Nullable
    private final ApplicationInstanceController instanceController;

    public CommandLineContext(@Nullable ApplicationInstanceController instanceController) {
        this.instanceController = instanceController;
    }

    @NotNull
    public Map<String, Object> getContext() {
        return contextParameter;
    }

    @Nullable
    public <T> T getContextParameter(String name) {
        return (T) contextParameter.get(name);
    }

    public void setContextParameter(@NotNull String name, @NotNull Object value) {
        contextParameter.put(name, value);
    }


    public void addResult(@NotNull Object result) {
        this.results.add(result);
    }

    @NotNull
    public List<Object> getResults() {
        return List.copyOf(results);
    }

    public void addCloseHandler(@NotNull Runnable closeHandler) {
        closeHandlers.add(closeHandler);
    }


    @Nullable
    public ApplicationInstanceController getInstanceController() {
        return instanceController;
    }

    @Override
    public void close() {
        for (Runnable closeHandler : closeHandlers) {
            try {
                closeHandler.run();
            } catch (Exception e) {
                log.error("Error during close cli context: " + e.getMessage(), e);
            }
        }
    }
}
