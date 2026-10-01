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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class TextGetSetEditingSupport<T> extends EditingSupport {
    private final Function<T, String> getter;
    private final BiConsumer<T, String> setter;
    private final ColumnViewer viewer;

    public TextGetSetEditingSupport(
        @NotNull ColumnViewer viewer,
        @NotNull Function<T, String> getter,
        @NotNull BiConsumer<T, String> setter
    ) {
        super(viewer);
        this.viewer = viewer;
        this.getter = getter;
        this.setter = setter;
    }

    @NotNull
    @Override
    protected CellEditor getCellEditor(@NotNull Object element) {
        return new TextCellEditor((Composite) getViewer().getControl());
    }

    @Override
    protected boolean canEdit(@NotNull Object element) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    protected Object getValue(@NotNull Object element) {
        return getter.apply((T) element);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void setValue(@NotNull Object element, @Nullable Object value) {
        setter.accept((T) element, (String) value);
        viewer.update(element, null);
    }
}