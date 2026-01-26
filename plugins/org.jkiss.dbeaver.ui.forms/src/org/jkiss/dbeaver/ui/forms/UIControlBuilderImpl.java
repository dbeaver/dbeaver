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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

abstract sealed class UIControlBuilderImpl<B extends UIControlBuilder<B>, C extends Control> implements UIControlBuilder<B>
    permits UIControlBuilderImpl.ButtonBuilderImpl, UIControlBuilderImpl.ComboBuilderImpl, UIControlBuilderImpl.LabelBuilderImpl,
    UIControlBuilderImpl.TextBuilderImpl, UIPanelBuilderImpl {

    private UIObservable<Boolean> visible;
    private UIObservable<Boolean> enabled;
    private String tooltip;

    int alignX = SWT.BEGINNING;
    int alignY = SWT.CENTER;
    int widthHint = SWT.DEFAULT;
    int heightHint = SWT.DEFAULT;
    boolean grow = false;

    @NotNull
    @Override
    public B visible(@NotNull UIObservable<Boolean> binding) {
        visible = binding;
        return builder();
    }

    @NotNull
    @Override
    public B enabled(@NotNull UIObservable<Boolean> binding) {
        enabled = binding;
        return builder();
    }

    @NotNull
    @Override
    public B tooltip(@NotNull String value) {
        tooltip = value;
        return builder();
    }

    @NotNull
    @Override
    public B grow() {
        grow = true;
        return builder();
    }

    @NotNull
    @Override
    public B align(@NotNull UIAlignX x, @NotNull UIAlignY y) {
        alignX = x.toSWT();
        alignY = y.toSWT();
        return builder();
    }

    @NotNull
    @Override
    public B align(@NotNull UIAlignX x) {
        alignX = x.toSWT();
        return builder();
    }

    @NotNull
    @Override
    public B align(@NotNull UIAlignY y) {
        alignY = y.toSWT();
        return builder();
    }

    @NotNull
    @Override
    public B hint(int width, int height) {
        widthHint = width;
        heightHint = height;
        return builder();
    }

    @NotNull
    @Override
    public B accept(@NotNull Consumer<? super B> consumer) {
        B builder = builder();
        consumer.accept(builder);
        return builder;
    }

    @NotNull
    C build(@NotNull DataBindingContext context, @NotNull Composite parent, @Nullable UIRowBuilderImpl row) {
        C control = create(context, parent);
        bind(context, control, row);
        return control;
    }

    @NotNull
    protected abstract C create(@NotNull DataBindingContext context, @NotNull Composite parent);

    @Nullable
    protected Point preferredSize(@NotNull C control) {
        return null;
    }

    protected void bind(@NotNull DataBindingContext context, @NotNull C control, @Nullable UIRowBuilderImpl row) {
        if (row != null && row.visible != null || visible != null) {
            // FIXME: Initially non-visible controls occupy space
            var binding = UIObservables.and(row != null ? row.visible : null, visible);
            var delegate = delegate(binding);
            delegate.addValueChangeListener(event -> {
                var data = (GridData) control.getLayoutData();
                var value = (boolean) binding.get();
                if (data.exclude == value) {
                    data.exclude = !value;
                    control.requestLayout();
                }
            });
            context.bindValue(WidgetProperties.visible().observe(control), delegate);
        }
        if (row != null && row.enabled != null || enabled != null) {
            var binding = UIObservables.and(row != null ? row.enabled : null, enabled);
            context.bindValue(WidgetProperties.enabled().observe(control), delegate(binding));
        }
        if (tooltip != null) {
            control.setToolTipText(tooltip);
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private B builder() {
        return (B) this;
    }

    @NotNull
    private static <T> IObservableValue<T> delegate(@NotNull UIObservable<T> observable) {
        return ((UIObservableImpl<T>) observable).delegate();
    }

    static final class LabelBuilderImpl extends UIControlBuilderImpl<LabelBuilder, Label> implements LabelBuilder {
        private final String text;
        private final int style;

        LabelBuilderImpl(@NotNull String text, int style) {
            this.text = text;
            this.style = style;
        }

        @NotNull
        @Override
        protected Label create(@NotNull DataBindingContext context, @NotNull Composite parent) {
            return UIControlFactory.createLabel(parent, style, text);
        }
    }

    static final class TextBuilderImpl<T> extends UIControlBuilderImpl<TextBuilder<T>, Text> implements TextBuilder<T> {
        private final int style;
        private final UIObservable<T> text;
        private Function<? super String, IStatus> toModelValidator;
        private Function<? super String, ? extends T> toModelConverter;
        private Function<? super T, String> fromModelConverter;

        TextBuilderImpl(int style, @NotNull UIObservable<T> text) {
            this.style = style;
            this.text = text;
        }

        @NotNull
        @Override
        public TextBuilder<T> toModel(
            @NotNull Function<? super String, IStatus> afterGetValidator,
            @NotNull Function<? super String, ? extends T> targetToModelConverter
        ) {
            this.toModelValidator = afterGetValidator;
            this.toModelConverter = targetToModelConverter;
            return this;
        }

        @NotNull
        @Override
        public TextBuilder<T> fromModel(@NotNull Function<? super T, String> modelToTargetConverter) {
            this.fromModelConverter = modelToTargetConverter;
            return this;
        }

        @NotNull
        @Override
        protected Text create(@NotNull DataBindingContext context, @NotNull Composite parent) {
            return UIControlFactory.createText(parent, style);
        }

        @NotNull
        @Override
        protected Point preferredSize(@NotNull Text control) {
            return new Point(UIUtils.getFontHeight(control) * 15, SWT.DEFAULT);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void bind(@NotNull DataBindingContext context, @NotNull Text control, @Nullable UIRowBuilderImpl row) {
            super.bind(context, control, row);

            UpdateValueStrategy<String, ? extends T> toModelStrategy = null;
            UpdateValueStrategy<? super T, String> fromModelStrategy = null;

            if (toModelConverter != null) {
                toModelStrategy
                    = (UpdateValueStrategy<String, ? extends T>) UpdateValueStrategy.create(IConverter.create(toModelConverter));
                toModelStrategy.setAfterGetValidator(toModelValidator::apply);
            }

            if (fromModelConverter != null) {
                fromModelStrategy = UpdateValueStrategy.create(IConverter.create(fromModelConverter));
            }

            var target = WidgetProperties.text(SWT.Modify).observe(control);
            var binding = context.bindValue(target, delegate(text), toModelStrategy, fromModelStrategy);

            ControlDecorationSupport.create(binding, SWT.TOP | SWT.LEFT);
        }
    }

    static final class ButtonBuilderImpl extends UIControlBuilderImpl<ButtonBuilder, Button> implements ButtonBuilder {
        private final String text;
        private final Consumer<SelectionEvent> onSelect;
        private final int style;
        private UIObservable<Boolean> selected;

        ButtonBuilderImpl(@NotNull String text, @Nullable Consumer<SelectionEvent> onSelect, int style) {
            this.text = text;
            this.onSelect = onSelect;
            this.style = style;
        }

        @NotNull
        @Override
        public ButtonBuilder selected(@NotNull UIObservable<Boolean> binding) {
            selected = binding;
            return this;
        }

        @NotNull
        @Override
        protected Button create(@NotNull DataBindingContext context, @NotNull Composite parent) {
            Button button = UIControlFactory.createButton(parent, style, text);
            if (onSelect != null) {
                button.addSelectionListener(SelectionListener.widgetSelectedAdapter(onSelect));
            }
            return button;
        }

        @NotNull
        @Override
        protected Point preferredSize(@NotNull Button control) {
            return new Point(UIUtils.getDialogButtonWidth(control), SWT.DEFAULT);
        }

        @Override
        protected void bind(@NotNull DataBindingContext context, @NotNull Button control, @Nullable UIRowBuilderImpl row) {
            super.bind(context, control, row);
            if (selected != null) {
                context.bindValue(WidgetProperties.buttonSelection().observe(control), delegate(selected));
            }
        }
    }

    static final class ComboBuilderImpl<T> extends UIControlBuilderImpl<ComboBuilder<T>, Combo> implements ComboBuilder<T> {
        private final UIObservable<T> binding;
        private final Function<? super T, String> converter;
        private final List<? extends T> items;
        private final int style;

        public ComboBuilderImpl(
            @NotNull UIObservable<T> binding,
            @NotNull Function<? super T, String> converter,
            @NotNull List<? extends T> items,
            int style
        ) {
            this.binding = binding;
            this.converter = converter;
            this.items = List.copyOf(items);
            this.style = style;
        }

        @NotNull
        @Override
        protected Combo create(@NotNull DataBindingContext context, @NotNull Composite parent) {
            Combo combo = UIControlFactory.createCombo(parent, style);
            for (T item : items) {
                combo.add(converter.apply(item));
            }
            return combo;
        }

        @Override
        protected void bind(@NotNull DataBindingContext context, @NotNull Combo control, @Nullable UIRowBuilderImpl row) {
            super.bind(context, control, row);

            context.bindValue(
                WidgetProperties.singleSelectionIndex().observe(control),
                delegate(binding),
                UpdateValueStrategy.create(IConverter.create(items::get)),
                UpdateValueStrategy.create(IConverter.create(items::indexOf))
            );
        }
    }
}
