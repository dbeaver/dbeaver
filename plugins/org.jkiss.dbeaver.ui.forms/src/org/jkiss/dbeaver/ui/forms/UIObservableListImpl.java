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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class UIObservableListImpl<E> extends AbstractList<E> implements UIObservableList<E> {
    private final List<E> values;
    private final Class<E> type;
    private final List<Runnable> listeners = new ArrayList<>();

    private UIObservableListImpl(@NotNull Collection<? extends E> values, @NotNull Class<E> type) {
        this.values = new ArrayList<>(values);
        this.type = type;
    }

    @NotNull
    static <E> UIObservableListImpl<E> of(@NotNull Collection<? extends E> values, @NotNull Class<E> type) {
        return new UIObservableListImpl<>(values, type);
    }

    @Nullable
    @Override
    public E get(int index) {
        return values.get(index);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Nullable
    @Override
    public E set(int index, @Nullable E element) {
        E old = values.set(index, element);
        changed();
        return old;
    }

    @Override
    public void add(int index, @Nullable E element) {
        values.add(index, element);
        modCount++;
        changed();
    }

    @Nullable
    @Override
    public E remove(int index) {
        E old = values.remove(index);
        modCount++;
        changed();
        return old;
    }

    @NotNull
    @Override
    public Class<E> type() {
        return type;
    }

    void addListener(@NotNull Runnable listener) {
        listeners.add(listener);
    }

    void removeListener(@NotNull Runnable listener) {
        listeners.remove(listener);
    }

    private void changed() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }
}
