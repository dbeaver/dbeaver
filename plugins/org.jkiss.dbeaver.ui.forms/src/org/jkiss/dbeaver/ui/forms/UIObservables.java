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

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Objects;

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
        ComputedValue<Boolean> computed = new ComputedValue<>() {
            @NotNull
            @Override
            protected Boolean calculate() {
                return first.get() && second.get();
            }
        };
        return new UIObservableImpl<>(computed, Boolean.class);
    }

    @NotNull
    public static <T> UIObservable<Boolean> equals(@NotNull UIObservable<T> observable, @NotNull T value) {
        ComputedValue<Boolean> computed = new ComputedValue<>() {
            @NotNull
            @Override
            protected Boolean calculate() {
                return Objects.equals(observable.get(), value);
            }

            @Override
            protected void doSetValue(@NotNull Boolean newValue) {
                if (Objects.equals(observable.get(), newValue)) {
                    return;
                }
                if (newValue) {
                    observable.set(value);
                    fireValueChange(Diffs.createValueDiff(false, true));
                }
            }
        };
        return new UIObservableImpl<>(computed, Boolean.class);
    }
}
