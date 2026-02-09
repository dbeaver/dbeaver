/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import java.util.List;
import java.util.Map;

public interface CLIContext {
    @NotNull
    Map<String, Object> getContextParameters();

    @Nullable
    <T> T getContextParameter(String name);

    void setContextParameter(@NotNull String name, @NotNull Object value);

    void addResult(@NotNull String result);

    @NotNull
    List<String> getResults();

    void addCloseHandler(@NotNull Runnable closeHandler);

    @Nullable
    CLIProcessResult.PostAction getPostAction();

    void setPostAction(@Nullable CLIProcessResult.PostAction postAction);
}
