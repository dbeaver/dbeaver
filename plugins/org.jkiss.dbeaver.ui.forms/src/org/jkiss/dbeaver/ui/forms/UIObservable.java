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

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.function.Supplier;

/**
 * A value whose changes can be observed.
 *
 * @param <T> the type of the value
 */
@SuppressWarnings("CheckStyle")
public sealed interface UIObservable<T> permits UIObservableImpl {
    @NotNull
    static UIObservable<Byte> of(byte value) {
        return of(value, byte.class);
    }

    @NotNull
    static UIObservable<Short> of(short value) {
        return of(value, short.class);
    }

    @NotNull
    static UIObservable<Integer> of(int value) {
        return of(value, int.class);
    }

    @NotNull
    static UIObservable<Long> of(long value) {
        return of(value, long.class);
    }

    @NotNull
    static UIObservable<Float> of(float value) {
        return of(value, float.class);
    }

    @NotNull
    static UIObservable<Double> of(double value) {
        return of(value, double.class);
    }

    @NotNull
    static UIObservable<Boolean> of(boolean value) {
        return of(value, boolean.class);
    }

    @NotNull
    static UIObservable<Character> of(char value) {
        return of(value, char.class);
    }

    @NotNull
    static UIObservable<String> of(@Nullable String value) {
        return of(value, String.class);
    }

    @NotNull
    static <T extends Enum<T>> UIObservable<T> of(@NotNull T value) {
        return of(value, value.getDeclaringClass());
    }

    @NotNull
    static <T> UIObservable<T> of(@Nullable T value, @NotNull Class<T> type) {
        return new UIObservableImpl<>(value, type);
    }

    @NotNull
    static UIObservable<Boolean> predicate(@NotNull Supplier<Boolean> supplier) {
        return computed(supplier, Boolean.class);
    }

    @NotNull
    static <T> UIObservable<T> computed(@NotNull Supplier<T> supplier, @NotNull Class<T> type) {
        return new UIObservableImpl<>(ComputedValue.create(supplier), type);
    }

    /**
     * Gets the current value.
     *
     * @return the current value
     */
    T get();

    /**
     * Sets a new value.
     *
     * @param value the new value
     */
    void set(T value);

    /**
     * Gets the type of the value.
     *
     * @return the type of the value
     */
    @NotNull
    Class<T> type();
}
