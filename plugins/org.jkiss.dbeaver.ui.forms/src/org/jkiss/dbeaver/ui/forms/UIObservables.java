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

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Various utilities for working with {@link UIObservable}.
 */
public final class UIObservables {
    private UIObservables() {
    }

    @NotNull
    public static UIObservable<Boolean> and(
        @Nullable UIObservable<Boolean> first,
        @Nullable UIObservable<Boolean> second
    ) {
        if (first == null && second == null) {
            throw new IllegalArgumentException("Either first or second must not be null");
        }
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return computed(() -> first.get() && second.get(), Boolean.class);
    }

    @NotNull
    public static <T> UIObservable<Boolean> equals(@NotNull UIObservable<T> observable, @NotNull T value) {
        return new UIObservableImpl<>(ObservableValue.computed(
            () -> Objects.equals(observable.get(), value),
            (old, selected) -> {
                if (selected) {
                    observable.set(value);
                }
            }
        ), Boolean.class);
    }

    @NotNull
    public static UIObservable<Boolean> predicate(@NotNull Supplier<Boolean> supplier) {
        return computed(supplier, Boolean.class);
    }

    @NotNull
    public static <T> UIObservable<T> computed(@NotNull Supplier<T> supplier, @NotNull Class<T> type) {
        return new UIObservableImpl<>(ObservableValue.computed(supplier), type);
    }
}
