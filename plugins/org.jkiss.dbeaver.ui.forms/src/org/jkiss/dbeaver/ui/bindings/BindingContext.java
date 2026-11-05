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

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Manages widget/model synchronization for one forms panel. */
public final class BindingContext {
    private final List<Runnable> cleanup = new ArrayList<>();

    public <T> void bind(
        @NotNull Control control,
        @NotNull ObservableValue<T> model,
        @NotNull Consumer<T> writeControl,
        @Nullable Supplier<T> readControl,
        int eventType
    ) {
        // SWT setters can fire events synchronously. Do not echo an update back to its source.
        boolean[] updating = {false};
        BiConsumer<T, T> change = (oldValue, newValue) -> {
            if (!updating[0] && !control.isDisposed()) {
                updating[0] = true;
                try {
                    writeControl.accept(newValue);
                } finally {
                    updating[0] = false;
                }
            }
        };
        model.addListener(change);
        cleanup.add(() -> model.removeListener(change));
        if (readControl != null) {
            Listener listener = event -> {
                if (updating[0]) {
                    return;
                }
                updating[0] = true;
                try {
                    T value = readControl.get();
                    if (!Objects.equals(value, model.get())) {
                        model.set(value);
                    }
                } finally {
                    updating[0] = false;
                }
            };
            control.addListener(eventType, listener);
            cleanup.add(() -> {
                if (!control.isDisposed()) {
                    control.removeListener(eventType, listener);
                }
            });
        }
        change.accept(null, model.get());
    }

    public void dispose() {
        for (Runnable action : cleanup) {
            action.run();
        }
        cleanup.clear();
    }
}
