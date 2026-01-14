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

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

@SuppressWarnings("CheckStyle")
final class UIObservableImpl<T> implements UIObservable<T> {
    private final IObservableValue<T> delegate;
    private final Class<T> type;

    UIObservableImpl(@NotNull IObservableValue<T> delegate, Class<T> type) {
        this.delegate = delegate;
        this.type = type;
    }

    UIObservableImpl(@Nullable T value, @NotNull Class<T> type) {
        this.delegate = new WritableValue<>(value, type);
        this.type = type;
    }

    @Override
    public T get() {
        return delegate.getValue();
    }

    @Override
    public void set(T value) {
        delegate.setValue(value);
    }

    @NotNull
    @Override
    public Class<T> type() {
        return type;
    }

    @NotNull
    IObservableValue<T> delegate() {
        return delegate;
    }
}
