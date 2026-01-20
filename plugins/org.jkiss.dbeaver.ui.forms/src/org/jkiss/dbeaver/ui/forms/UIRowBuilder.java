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

import org.eclipse.core.databinding.conversion.text.NumberToStringConverter;
import org.eclipse.core.databinding.conversion.text.StringToNumberConverter;
import org.eclipse.core.internal.databinding.validation.StringToIntegerValidator;
import org.eclipse.swt.events.SelectionEvent;
import org.jkiss.code.NotNull;

import java.text.NumberFormat;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The builder for a row inside a panel.
 */
public sealed interface UIRowBuilder permits UIRowBuilderImpl {
    @NotNull
    static <T> Consumer<T> identityConsumer() {
        return t -> {/* does nothing */};
    }

    @NotNull
    UIRowBuilder enabled(@NotNull UIObservable<Boolean> binding);

    @NotNull
    UIRowBuilder visible(@NotNull UIObservable<Boolean> binding);

    @NotNull
    UIRowBuilder panel(@NotNull Consumer<? super UIPanelBuilder> handler);

    @NotNull
    UIRowBuilder group(@NotNull String text, @NotNull Consumer<? super UIPanelBuilder> handler);

    @NotNull
    UIRowBuilder expandableGroup(@NotNull String text, boolean expanded, @NotNull Consumer<? super UIPanelBuilder> handler);

    @NotNull
    UIRowBuilder label(@NotNull String text, @NotNull Consumer<? super UIControlBuilder.LabelBuilder> handler);

    @NotNull
    default UIRowBuilder label(@NotNull String text) {
        return label(text, identityConsumer());
    }

    @NotNull
    UIRowBuilder button(
        @NotNull String text,
        @NotNull Consumer<SelectionEvent> onSelect,
        @NotNull Consumer<? super UIControlBuilder.ButtonBuilder> handler
    );

    @NotNull
    default UIRowBuilder button(
        @NotNull String text,
        @NotNull Consumer<SelectionEvent> onSelect
    ) {
        return button(text, onSelect, identityConsumer());
    }

    @NotNull
    UIRowBuilder radioButton(@NotNull String text, @NotNull Consumer<? super UIControlBuilder.ButtonBuilder> handler);

    @NotNull
    UIRowBuilder checkBox(@NotNull String text, @NotNull Consumer<? super UIControlBuilder.ButtonBuilder> handler);

    @NotNull
    <T> UIRowBuilder textField(@NotNull UIObservable<T> binding, @NotNull Consumer<? super UIControlBuilder.TextBuilder<T>> handler);

    @NotNull
    default <T> UIRowBuilder textField(@NotNull UIObservable<T> binding) {
        return textField(binding, identityConsumer());
    }

    @NotNull
    <T> UIRowBuilder passwordField(@NotNull UIObservable<T> binding, @NotNull Consumer<? super UIControlBuilder.TextBuilder<T>> handler);

    @NotNull
    default <T> UIRowBuilder passwordField(@NotNull UIObservable<T> binding) {
        return passwordField(binding, identityConsumer());
    }

    @NotNull
    default UIRowBuilder intTextField(
        @NotNull UIObservable<? super Integer> binding,
        @NotNull Consumer<? super UIControlBuilder.TextBuilder<? super Integer>> handler
    ) {
        var format = NumberFormat.getIntegerInstance();
        format.setGroupingUsed(false);

        var toModelConverter = StringToNumberConverter.toInteger(true);
        var toModelValidator = new StringToIntegerValidator(toModelConverter);
        var fromModelConverter = NumberToStringConverter.fromInteger(format, true);
        return textField(binding, tb -> {
            handler.accept(tb);
            tb.toModel(toModelValidator::validate, toModelConverter::convert);
            tb.fromModel(fromModelConverter::convert);
        });
    }

    @NotNull
    default UIRowBuilder intTextField(@NotNull UIObservable<? super Integer> binding) {
        return intTextField(binding, identityConsumer());
    }

    @NotNull
    <T> UIRowBuilder comboBox(
        @NotNull List<? extends T> items,
        @NotNull UIObservable<T> binding,
        @NotNull Function<? super T, String> converter,
        @NotNull Consumer<? super UIControlBuilder.ComboBuilder<T>> handler
    );

    @NotNull
    default <T> UIRowBuilder comboBox(
        @NotNull List<? extends T> items,
        @NotNull UIObservable<T> binding,
        @NotNull Function<? super T, String> converter
    ) {
        return comboBox(items, binding, converter, identityConsumer());
    }

    @NotNull
    default UIRowBuilder comboBox(
        @NotNull List<? extends String> items,
        @NotNull UIObservable<? super String> binding
    ) {
        return comboBox(items, binding, Object::toString, identityConsumer());
    }

    @NotNull
    default <T extends Enum<T>> UIRowBuilder comboBox(
        @NotNull UIObservable<T> binding,
        @NotNull Function<? super T, String> converter,
        @NotNull Consumer<? super UIControlBuilder.ComboBuilder<T>> handler
    ) {
        var items = Stream.of(binding.type().getEnumConstants())
            .map(value -> (T) value)
            .toList();

        return comboBox(items, binding, converter, handler);
    }

    @NotNull
    default <T extends Enum<T>> UIRowBuilder comboBox(
        @NotNull UIObservable<T> binding,
        @NotNull Function<? super T, String> converter
    ) {
        return comboBox(binding, converter, identityConsumer());
    }
}
