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
package org.jkiss.dbeaver.ui.forms;

import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.conversion.text.NumberToStringConverter;
import org.eclipse.core.databinding.conversion.text.StringToNumberConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.validation.StringToIntegerValidator;
import org.eclipse.swt.events.SelectionEvent;
import org.jkiss.code.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The builder for a row inside a panel.
 */
public sealed interface RowBuilder permits RowBuilderImpl {
    @NotNull
    static <T> IConverter<? super T, ? extends T> identityConverter() {
        return IConverter.create(Function.identity());
    }

    @NotNull
    static <T> Consumer<T> identityConsumer() {
        return t -> {/* does nothing */};
    }

    @NotNull
    RowBuilder enabled(@NotNull IObservableValue<Boolean> binding);

    @NotNull
    RowBuilder visible(@NotNull IObservableValue<Boolean> binding);

    @NotNull
    RowBuilder panel(@NotNull Consumer<? super PanelBuilder> handler);

    @NotNull
    RowBuilder group(@NotNull String text, @NotNull Consumer<? super PanelBuilder> handler);

    @NotNull
    RowBuilder expandableGroup(@NotNull String text, boolean expanded, @NotNull Consumer<? super PanelBuilder> handler);

    @NotNull
    RowBuilder label(@NotNull String text, @NotNull Consumer<? super ControlBuilder.LabelBuilder> handler);

    @NotNull
    default RowBuilder label(@NotNull String text) {
        return label(text, identityConsumer());
    }

    @NotNull
    default RowBuilder controlLabel(@NotNull String text) {
        return label(text + ":");
    }

    @NotNull
    RowBuilder button(
        @NotNull String text,
        @NotNull Consumer<SelectionEvent> onSelect,
        @NotNull Consumer<? super ControlBuilder.ButtonBuilder> handler
    );

    @NotNull
    default RowBuilder button(
        @NotNull String text,
        @NotNull Consumer<SelectionEvent> onSelect
    ) {
        return button(text, onSelect, identityConsumer());
    }

    @NotNull
    RowBuilder radioButton(@NotNull String text, @NotNull Consumer<? super ControlBuilder.ButtonBuilder> handler);

    @NotNull
    RowBuilder checkBox(@NotNull String text, @NotNull Consumer<? super ControlBuilder.ButtonBuilder> handler);

    @NotNull
    <T> RowBuilder textField(@NotNull IObservableValue<T> binding, @NotNull Consumer<? super ControlBuilder.TextBuilder<T>> handler);

    @NotNull
    default <T> RowBuilder textField(@NotNull IObservableValue<T> binding) {
        return textField(binding, identityConsumer());
    }

    @NotNull
    <T> RowBuilder passwordField(@NotNull IObservableValue<T> binding, @NotNull Consumer<? super ControlBuilder.TextBuilder<T>> handler);

    @NotNull
    default <T> RowBuilder passwordField(@NotNull IObservableValue<T> binding) {
        return passwordField(binding, identityConsumer());
    }

    @NotNull
    default RowBuilder intTextField(
        @NotNull IObservableValue<? super Integer> binding,
        @NotNull Consumer<? super ControlBuilder.TextBuilder<? super Integer>> handler
    ) {
        var toModelConverter = StringToNumberConverter.toInteger(true);
        var toModelValidator = new StringToIntegerValidator(toModelConverter);
        var fromModelConverter = NumberToStringConverter.fromInteger(true);
        return textField(binding, tb -> {
            handler.accept(tb);
            tb.toModel(toModelValidator, toModelConverter);
            tb.fromModel(fromModelConverter);
        });
    }

    @NotNull
    default RowBuilder intTextField(@NotNull IObservableValue<? super Integer> binding) {
        return intTextField(binding, identityConsumer());
    }

    @NotNull
    <T> RowBuilder comboBox(
        @NotNull List<? extends T> items,
        @NotNull IObservableValue<T> binding,
        @NotNull IConverter<? super T, String> converter,
        @NotNull Consumer<? super ControlBuilder.ComboBuilder<T>> handler
    );

    @NotNull
    default <T> RowBuilder comboBox(
        @NotNull List<? extends T> items,
        @NotNull IObservableValue<T> binding,
        @NotNull IConverter<? super T, String> converter
    ) {
        return comboBox(items, binding, converter, identityConsumer());
    }

    @NotNull
    default RowBuilder comboBox(
        @NotNull List<? extends String> items,
        @NotNull IObservableValue<? super String> binding
    ) {
        return comboBox(items, binding, IConverter.create(Object::toString), identityConsumer());
    }

    @NotNull
    default <T extends Enum<T>> RowBuilder comboBox(
        @NotNull IObservableValue<T> binding,
        @NotNull IConverter<? super T, String> converter,
        @NotNull Consumer<? super ControlBuilder.ComboBuilder<T>> handler
    ) {
        if (!(binding.getValueType() instanceof Class<?> cls)) {
            throw new IllegalArgumentException("Binding must have its value type set");
        }

        @SuppressWarnings("unchecked")
        var items = Stream.of(cls.getEnumConstants())
            .map(value -> (T) value)
            .toList();

        return comboBox(items, binding, converter, handler);
    }

    @NotNull
    default <T extends Enum<T>> RowBuilder comboBox(
        @NotNull IObservableValue<T> binding,
        @NotNull IConverter<? super T, String> converter
    ) {
        return comboBox(binding, converter, identityConsumer());
    }

    @NotNull
    RowBuilder comment(@NotNull String text);
}
