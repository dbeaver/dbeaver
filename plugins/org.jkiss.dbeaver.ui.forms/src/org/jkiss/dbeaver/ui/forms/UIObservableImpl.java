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

import java.util.function.BiConsumer;

@SuppressWarnings("CheckStyle")
record UIObservableImpl<T>(@NotNull IObservableValue<T> delegate, @NotNull Class<T> type) implements UIObservable<T> {
    @NotNull
    static <T> UIObservableImpl<T> of(@Nullable T value, @NotNull Class<T> type) {
        return new UIObservableImpl<>(new WritableValue<>(value, type), type);
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

    @Override
    public void addChangeListener(@NotNull BiConsumer<T, T> listener) {
        // TODO currently used to listen to changes in multiple controls.
        //  Would be nice to introduce a generic "validation" callback
        //  that gets invoked recursively when let's say a panel's widget
        //  changes its validation state. Or at least gets modified without
        //  validating anything.
        delegate.addValueChangeListener(event -> listener.accept(event.diff.getOldValue(), event.diff.getNewValue()));
    }
}
