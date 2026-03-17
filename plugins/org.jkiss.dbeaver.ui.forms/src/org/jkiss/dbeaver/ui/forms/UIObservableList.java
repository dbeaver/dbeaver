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
package org.jkiss.dbeaver.ui.forms;

import org.jkiss.code.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * A list whose changes can be observed.
 *
 * @param <E> the type of elements in this list
 */
public sealed interface UIObservableList<E> extends List<E> permits UIObservableListImpl {
    @NotNull
    static <E> UIObservableList<E> of(@NotNull Class<E> type) {
        return UIObservableListImpl.of(List.of(), type);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    static <E> UIObservableList<E> copyOf(@NotNull Collection<? extends E> c, @NotNull Class<E> type) {
        return UIObservableListImpl.of((Collection<E>) c, type);
    }

    /**
     * Gets the type of this list's elements.
     *
     * @return the type of the value
     */
    @NotNull
    Class<E> type();
}
