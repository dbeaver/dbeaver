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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.jkiss.code.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

final class RowBuilderImpl implements RowBuilder {
    final List<ControlBuilderImpl<?, ?>> controls = new ArrayList<>();
    final int indent;

    Observable<Boolean> visible;
    Observable<Boolean> enabled;

    RowBuilderImpl(int indent) {
        this.indent = indent;
    }

    @NotNull
    @Override
    public RowBuilder visible(@NotNull Observable<Boolean> binding) {
        visible = binding;
        return this;
    }

    @NotNull
    @Override
    public RowBuilder enabled(@NotNull Observable<Boolean> binding) {
        enabled = binding;
        return this;
    }

    @NotNull
    @Override
    public RowBuilder panel(@NotNull Consumer<? super PanelBuilder> handler) {
        var builder = PanelBuilderImpl.panel();
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public RowBuilder group(@NotNull String text, @NotNull Consumer<? super PanelBuilder> handler) {
        var builder = PanelBuilderImpl.group(text);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public RowBuilder expandableGroup(@NotNull String text, boolean expanded, @NotNull Consumer<? super PanelBuilder> handler) {
        var builder = PanelBuilderImpl.expandableGroup(text, expanded);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public RowBuilder label(@NotNull String text, @NotNull Consumer<? super ControlBuilder.LabelBuilder> handler) {
        var builder = new ControlBuilderImpl.LabelBuilderImpl(text, SWT.NONE);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public RowBuilder button(
        @NotNull String text,
        @NotNull Consumer<SelectionEvent> onSelect,
        @NotNull Consumer<? super ControlBuilder.ButtonBuilder> handler
    ) {
        var builder = new ControlBuilderImpl.ButtonBuilderImpl(text, onSelect, SWT.BORDER);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public RowBuilder radioButton(@NotNull String text, @NotNull Consumer<? super ControlBuilder.ButtonBuilder> handler) {
        var builder = new ControlBuilderImpl.ButtonBuilderImpl(text, null, SWT.BORDER | SWT.RADIO);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public RowBuilder checkBox(@NotNull String text, @NotNull Consumer<? super ControlBuilder.ButtonBuilder> handler) {
        var builder = new ControlBuilderImpl.ButtonBuilderImpl(text, null, SWT.BORDER | SWT.CHECK);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public <T> RowBuilder textField(@NotNull Observable<T> binding, @NotNull Consumer<? super ControlBuilder.TextBuilder<T>> handler) {
        var builder = new ControlBuilderImpl.TextBuilderImpl<T>(SWT.BORDER, binding);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public <T> RowBuilder passwordField(@NotNull Observable<T> binding, @NotNull Consumer<? super ControlBuilder.TextBuilder<T>> handler) {
        var builder = new ControlBuilderImpl.TextBuilderImpl<T>(SWT.BORDER | SWT.PASSWORD, binding);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public <T> RowBuilder comboBox(
        @NotNull List<? extends T> items,
        @NotNull Observable<T> binding,
        @NotNull Function<? super T, String> converter,
        @NotNull Consumer<? super ControlBuilder.ComboBuilder<T>> handler
    ) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Enum doesn't have any constants");
        }

        var builder = new ControlBuilderImpl.ComboBuilderImpl<T>(binding, converter, items, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public RowBuilder comment(@NotNull String text) {
        controls.add(new ControlBuilderImpl.CommentBuilderImpl(text));
        return this;
    }
}
