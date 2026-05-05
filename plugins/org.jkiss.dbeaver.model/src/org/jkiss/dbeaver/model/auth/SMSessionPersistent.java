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

package org.jkiss.dbeaver.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.function.ThrowableConsumer;
import org.jkiss.utils.function.ThrowableFunction;

import java.util.Map;

public interface SMSessionPersistent extends SMSession {
    @NotNull
    Map<String, Object> getAttributes();

    @Nullable
    <T> T getAttribute(@NotNull String name);

    @NotNull
    <T, E extends Exception> T getAttribute(
        @NotNull String name,
        @NotNull ThrowableFunction<String, T, E> creator,
        @Nullable ThrowableConsumer<T, E> disposer
    ) throws E;

    void setAttribute(@NotNull String name, @Nullable Object value);

    @Nullable
    Object removeAttribute(@NotNull String name);
}
