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

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jkiss.code.NotNull;

import java.util.function.Function;

/**
 * Value validator
 *
 * @param <T> value type
 */
@FunctionalInterface
public interface UIValidator<T> extends Function<T, IStatus> {
    @NotNull
    static IStatus error(@NotNull String message) {
        return ValidationStatus.error(message);
    }

    @NotNull
    static IStatus error(@NotNull String message, @NotNull Throwable exception) {
        return ValidationStatus.error(message, exception);
    }

    @NotNull
    static IStatus cancel(@NotNull String message) {
        return ValidationStatus.cancel(message);
    }

    @NotNull
    static IStatus warning(@NotNull String message) {
        return ValidationStatus.warning(message);
    }

    @NotNull
    static IStatus info(@NotNull String message) {
        return ValidationStatus.info(message);
    }

    @NotNull
    static IStatus ok() {
        return ValidationStatus.ok();
    }
}
