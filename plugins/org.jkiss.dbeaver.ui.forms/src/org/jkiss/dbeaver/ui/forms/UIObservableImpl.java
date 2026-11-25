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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.bindings.ObservableValue;

import java.util.function.BiConsumer;

@SuppressWarnings("CheckStyle")
record UIObservableImpl<T>(@NotNull ObservableValue<T> delegate, @NotNull Class<T> type) implements UIObservable<T> {
    @NotNull
    static <T> UIObservableImpl<T> of(@Nullable T value, @NotNull Class<T> type) {
        return new UIObservableImpl<>(ObservableValue.writable(value), type);
    }

    @Nullable
    @Override
    public T get() {
        return delegate.get();
    }

    @Override
    public void set(@Nullable T value) {
        delegate.set(value);
    }

    @NotNull
    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public void addChangeListener(@NotNull BiConsumer<T, T> listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeChangeListener(@NotNull BiConsumer<T, T> listener) {
        delegate.removeListener(listener);
    }

    @NotNull
    @Override
    public String toString() {
        return String.format("UIObservableImpl[value=%s, type=%s]", get(), type.getName());
    }
}
