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
package org.jkiss.dbeaver.ui.bindings;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Observable value with dynamically tracked dependencies. */
public final class ObservableValue<T> {
    // Tracks the current supplier's reads without mixing independent computations on other threads.
    // This is evaluation context, not synchronization: each observable graph remains thread-confined.
    private static final ThreadLocal<ObservableValue<?>> COMPUTING = new ThreadLocal<>();

    private final List<BiConsumer<T, T>> listeners = new ArrayList<>();
    private final List<Runnable> rawListeners = new ArrayList<>();
    private final List<ObservableValue<?>> dependencies = new ArrayList<>();
    private final Runnable dependencyChanged = this::refresh;
    private final Supplier<T> supplier;
    private final BiConsumer<T, T> setter;
    private T value;
    private boolean initialized;

    private ObservableValue(@Nullable T value, @Nullable Supplier<T> supplier, @Nullable BiConsumer<T, T> setter) {
        this.value = value;
        this.supplier = supplier;
        this.setter = setter;
    }

    @NotNull
    public static <T> ObservableValue<T> writable(@Nullable T value) {
        return new ObservableValue<>(value, null, null);
    }

    @NotNull
    public static <T> ObservableValue<T> computed(@NotNull Supplier<T> supplier) {
        return computed(supplier, null);
    }

    @NotNull
    public static <T> ObservableValue<T> computed(@NotNull Supplier<T> supplier, @Nullable BiConsumer<T, T> setter) {
        return new ObservableValue<>(null, supplier, setter);
    }

    @Nullable
    public T get() {
        if (supplier != null && !initialized) {
            refresh();
        }
        ObservableValue<?> parent = COMPUTING.get();
        if (parent != null && parent != this && !parent.dependencies.contains(this)) {
            parent.dependencies.add(this);
            rawListeners.add(parent.dependencyChanged);
        }
        return value;
    }

    public void set(@Nullable T newValue) {
        if (supplier != null) {
            if (setter == null) {
                throw new UnsupportedOperationException("Computed value is read-only");
            }
            setter.accept(get(), newValue);
            refresh();
        } else {
            update(newValue);
        }
    }

    public void addListener(@NotNull BiConsumer<T, T> listener) {
        get();
        listeners.add(listener);
    }

    public void removeListener(@NotNull BiConsumer<T, T> listener) {
        listeners.remove(listener);
    }

    private void refresh() {
        if (supplier == null) {
            return;
        }
        for (ObservableValue<?> dependency : dependencies) {
            dependency.rawListeners.remove(dependencyChanged);
        }
        dependencies.clear();
        ObservableValue<?> previous = COMPUTING.get();
        COMPUTING.set(this);
        try {
            T result = supplier.get();
            initialized = true;
            update(result);
        } finally {
            COMPUTING.set(previous);
        }
    }

    private void update(@Nullable T newValue) {
        T old = value;
        if (!Objects.equals(old, newValue)) {
            value = newValue;
            for (BiConsumer<T, T> listener : List.copyOf(listeners)) {
                listener.accept(old, newValue);
            }
            for (Runnable listener : List.copyOf(rawListeners)) {
                listener.run();
            }
        }
    }
}
