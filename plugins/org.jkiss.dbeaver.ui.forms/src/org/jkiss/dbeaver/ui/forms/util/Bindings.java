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
package org.jkiss.dbeaver.ui.forms.util;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Objects;

public final class Bindings {
    private Bindings() {
    }

    @NotNull
    public static IObservableValue<Boolean> and(
        @Nullable IObservableValue<Boolean> first,
        @Nullable IObservableValue<Boolean> second
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
        return ComputedValue.create(() -> first.getValue() && second.getValue());
    }

    @NotNull
    public static <T> IObservableValue<Boolean> select(@NotNull IObservableValue<T> observable, @NotNull T value) {
        return new ComputedValue<>() {
            @NotNull
            @Override
            protected Boolean calculate() {
                return Objects.equals(observable.getValue(), value);
            }

            @Override
            protected void doSetValue(@NotNull Boolean newValue) {
                if (Objects.equals(observable.getValue(), newValue)) {
                    return;
                }
                if (newValue) {
                    observable.setValue(value);
                    fireValueChange(Diffs.createValueDiff(false, true));
                }
            }
        };
    }

    @NotNull
    public static WritableValue<Boolean> of(boolean value) {
        return of(value, boolean.class);
    }

    @NotNull
    public static WritableValue<Integer> of(int value) {
        return of(value, int.class);
    }

    @NotNull
    public static WritableValue<String> of(@Nullable String value) {
        return of(value, String.class);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> WritableValue<T> of(@NotNull T value) {
        return of(value, (Class<T>) value.getClass());
    }

    @NotNull
    public static <T> WritableValue<T> of(@Nullable T value, @NotNull Class<T> type) {
        return new WritableValue<>(value, type);
    }
}
