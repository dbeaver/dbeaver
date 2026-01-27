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

final class UIRowBuilderImpl implements UIRowBuilder {
    final List<UIControlBuilderImpl<?, ?>> controls = new ArrayList<>();
    final int indent;

    UIObservable<Boolean> visible;
    UIObservable<Boolean> enabled;

    UIRowBuilderImpl(int indent) {
        this.indent = indent;
    }

    @NotNull
    @Override
    public UIRowBuilder visible(@NotNull UIObservable<Boolean> binding) {
        visible = binding;
        return this;
    }

    @NotNull
    @Override
    public UIRowBuilder enabled(@NotNull UIObservable<Boolean> binding) {
        enabled = binding;
        return this;
    }

    @NotNull
    @Override
    public UIRowBuilder panel(@NotNull Consumer<? super UIPanelBuilder> handler) {
        var builder = UIPanelBuilderImpl.panel();
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public UIRowBuilder group(@NotNull String text, @NotNull Consumer<? super UIPanelBuilder> handler) {
        var builder = UIPanelBuilderImpl.group(text);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public UIRowBuilder expandableGroup(@NotNull String text, boolean expanded, @NotNull Consumer<? super UIPanelBuilder> handler) {
        var builder = UIPanelBuilderImpl.expandableGroup(text, expanded);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public UIRowBuilder label(@NotNull String text, @NotNull Consumer<? super UIControlBuilder.LabelBuilder> handler) {
        var builder = new UIControlBuilderImpl.LabelBuilderImpl(text, SWT.NONE);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public UIRowBuilder button(
        @NotNull String text,
        @NotNull Consumer<SelectionEvent> onSelect,
        @NotNull Consumer<? super UIControlBuilder.ButtonBuilder> handler
    ) {
        var builder = new UIControlBuilderImpl.ButtonBuilderImpl(text, onSelect, SWT.NONE);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public UIRowBuilder radioButton(@NotNull String text, @NotNull Consumer<? super UIControlBuilder.ButtonBuilder> handler) {
        var builder = new UIControlBuilderImpl.ButtonBuilderImpl(text, null, SWT.RADIO);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public UIRowBuilder checkBox(@NotNull String text, @NotNull Consumer<? super UIControlBuilder.ButtonBuilder> handler) {
        var builder = new UIControlBuilderImpl.ButtonBuilderImpl(text, null, SWT.CHECK);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public <T> UIRowBuilder textField(@NotNull UIObservable<T> binding, @NotNull Consumer<? super UIControlBuilder.TextBuilder<T>> handler) {
        var builder = new UIControlBuilderImpl.TextBuilderImpl<T>(SWT.BORDER, binding);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public <T> UIRowBuilder passwordField(@NotNull UIObservable<T> binding, @NotNull Consumer<? super UIControlBuilder.TextBuilder<T>> handler) {
        var builder = new UIControlBuilderImpl.TextBuilderImpl<T>(SWT.BORDER | SWT.PASSWORD, binding);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

    @NotNull
    @Override
    public <T> UIRowBuilder comboBox(
        @NotNull List<? extends T> items,
        @NotNull UIObservable<T> binding,
        @NotNull Function<? super T, String> converter,
        @NotNull Consumer<? super UIControlBuilder.ComboBuilder<T>> handler
    ) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Enum doesn't have any constants");
        }

        var builder = new UIControlBuilderImpl.ComboBuilderImpl<T>(binding, converter, items, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        handler.accept(builder);
        controls.add(builder);
        return this;
    }

}
