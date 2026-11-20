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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.bindings.BindingContext;
import org.jkiss.dbeaver.ui.bindings.ObservableValue;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

abstract sealed class UIControlBuilderImpl<B extends UIControlBuilder<B>, C extends Control> implements UIControlBuilder<B>
    permits UIControlBuilderImpl.ButtonBuilderImpl, UIControlBuilderImpl.ComboBuilderImpl, UIControlBuilderImpl.ControlBuilderImpl,
    UIControlBuilderImpl.LabelBuilderImpl, UIControlBuilderImpl.LinkBuilderImpl, UIControlBuilderImpl.TextBuilderImpl, UIPanelBuilderImpl {

    private UIObservable<Boolean> visible;
    private UIObservable<Boolean> enabled;
    private UIObservable<Font> font;
    private UIObservable<String> tooltip;

    UIAlignX alignX = UIAlignX.LEFT;
    UIAlignY alignY = UIAlignY.CENTER;
    UIGrowX growX = UIGrowX.NEVER;
    UIGrowY growY = UIGrowY.NEVER;
    int widthHint = SWT.DEFAULT;
    int heightHint = SWT.DEFAULT;

    @NotNull
    @Override
    public B visible(@NotNull UIObservable<Boolean> value) {
        visible = value;
        return builder();
    }

    @NotNull
    @Override
    public B enabled(@NotNull UIObservable<Boolean> value) {
        enabled = value;
        return builder();
    }

    @NotNull
    @Override
    public B font(@NotNull UIObservable<Font> value) {
        font = value;
        return builder();
    }

    @NotNull
    @Override
    public B tooltip(@NotNull UIObservable<String> value) {
        tooltip = value;
        return builder();
    }

    @NotNull
    @Override
    public B grow(@NotNull UIGrowX x, @NotNull UIGrowY y) {
        growX = x;
        growY = y;
        return builder();
    }

    @NotNull
    @Override
    public B grow(@NotNull UIGrowX x) {
        growX = x;
        return builder();
    }

    @NotNull
    @Override
    public B grow(@NotNull UIGrowY y) {
        growY = y;
        return builder();
    }

    @NotNull
    @Override
    public B align(@NotNull UIAlignX x, @NotNull UIAlignY y) {
        alignX = x;
        alignY = y;
        return builder();
    }

    @NotNull
    @Override
    public B align(@NotNull UIAlignX x) {
        alignX = x;
        return builder();
    }

    @NotNull
    @Override
    public B align(@NotNull UIAlignY y) {
        alignY = y;
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
    C build(@NotNull BindingContext context, @NotNull Composite parent, @Nullable UIRowBuilderImpl row) {
        C control = create(context, parent);
        bind(context, control, row);
        return control;
    }

    @NotNull
    protected abstract C create(@NotNull BindingContext context, @NotNull Composite parent);

    boolean isInitiallyVisible(@Nullable UIRowBuilderImpl row) {
        return (row == null || row.visible == null || row.visible.get())
            && (visible == null || visible.get());
    }

    @Nullable
    protected Point preferredSize(@NotNull C control) {
        return null;
    }

    protected void bind(@NotNull BindingContext context, @NotNull C control, @Nullable UIRowBuilderImpl row) {
        if (row != null && row.visible != null || visible != null) {
            var binding = UIObservables.and(row != null ? row.visible : null, visible);
            var delegate = delegate(binding);
            var listener = (BiConsumer<Boolean, Boolean>) (oldValue, newValue) -> {
                var data = (GridData) control.getLayoutData();
                if (data != null && data.exclude == newValue) {
                    data.exclude = !newValue;
                    control.requestLayout();
                }
            };
            binding.addChangeListener(listener);
            control.addDisposeListener(event -> binding.removeChangeListener(listener));
            context.bind(control, delegate, control::setVisible, null, SWT.NONE);
        }
        if (row != null && row.enabled != null || enabled != null) {
            var binding = UIObservables.and(row != null ? row.enabled : null, enabled);
            context.bind(control, delegate(binding), control::setEnabled, null, SWT.NONE);
        }
        if (font != null) {
            context.bind(control, delegate(font), control::setFont, null, SWT.NONE);
        }
        if (tooltip != null) {
            context.bind(control, delegate(tooltip), control::setToolTipText, null, SWT.NONE);
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private B builder() {
        return (B) this;
    }

    @NotNull
    private static <T> ObservableValue<T> delegate(@NotNull UIObservable<T> observable) {
        return ((UIObservableImpl<T>) observable).delegate();
    }

    static final class LabelBuilderImpl extends UIControlBuilderImpl<LabelBuilder, Label> implements LabelBuilder {
        private UIObservable<String> text;
        private UIObservable<DBIcon> image;
        private int style = SWT.NONE;

        @NotNull
        @Override
        public LabelBuilder text(@Nullable UIObservable<String> text) {
            this.text = text;
            return this;
        }

        @NotNull
        @Override
        public LabelBuilder image(@Nullable UIObservable<DBIcon> image) {
            this.image = image;
            return this;
        }

        @NotNull
        @Override
        public LabelBuilder wrap() {
            style |= SWT.WRAP;
            return this;
        }

        @NotNull
        @Override
        protected Label create(@NotNull BindingContext context, @NotNull Composite parent) {
            return UIControlFactory.createLabel(parent, style);
        }

        @Override
        protected void bind(@NotNull BindingContext context, @NotNull Label control, @Nullable UIRowBuilderImpl row) {
            super.bind(context, control, row);
            if (text != null) {
                context.bind(control, delegate(text), value -> control.setText(value == null ? "" : value), null, SWT.NONE);
            }
            if (image != null) {
                var observable = image.map(DBeaverIcons::getImage, Image.class);
                context.bind(control, delegate(observable), control::setImage, null, SWT.NONE);
            }
        }
    }

    static final class LinkBuilderImpl extends UIControlBuilderImpl<LinkBuilder, Link> implements LinkBuilder {
        private final UIObservable<String> text;
        private final Consumer<SelectionEvent> onSelect;
        private final int style;

        LinkBuilderImpl(@NotNull UIObservable<String> text, @NotNull Consumer<SelectionEvent> onSelect, int style) {
            this.text = text;
            this.onSelect = onSelect;
            this.style = style;
        }

        @NotNull
        @Override
        protected Link create(@NotNull BindingContext context, @NotNull Composite parent) {
            Link link = UIControlFactory.createLink(parent, style);
            link.addSelectionListener(SelectionListener.widgetSelectedAdapter(onSelect));
            return link;
        }

        @Override
        protected void bind(@NotNull BindingContext context, @NotNull Link control, @Nullable UIRowBuilderImpl row) {
            super.bind(context, control, row);
            context.bind(control, delegate(text), value -> control.setText(value == null ? "" : value), null, SWT.NONE);
        }
    }

    static final class TextBuilderImpl<T> extends UIControlBuilderImpl<TextBuilder<T>, Text> implements TextBuilder<T> {
        private final int style;
        private final UIObservable<T> text;
        private UIObservable<String> message;
        private Function<? super String, IStatus> toModelValidator;
        private Function<? super String, ? extends T> toModelConverter;
        private Function<? super T, String> fromModelConverter;

        TextBuilderImpl(int style, @NotNull UIObservable<T> text) {
            this.style = style;
            this.text = text;
        }

        @NotNull
        @Override
        public TextBuilder<T> message(@NotNull UIObservable<String> value) {
            message = value;
            return this;
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
        protected Text create(@NotNull BindingContext context, @NotNull Composite parent) {
            return UIControlFactory.createText(parent, style);
        }

        @NotNull
        @Override
        protected Point preferredSize(@NotNull Text control) {
            return new Point(UIUtils.getFontHeight(control) * 15, SWT.DEFAULT);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void bind(@NotNull BindingContext context, @NotNull Text control, @Nullable UIRowBuilderImpl row) {
            super.bind(context, control, row);
            if (message != null) {
                context.bind(control, delegate(message), control::setMessage, null, SWT.NONE);
            }

            ControlDecoration decoration = new ControlDecoration(control, SWT.TOP | SWT.LEFT);
            decoration.hide();
            context.bind(control, delegate(text), value -> {
                String display = fromModelConverter != null ? fromModelConverter.apply(value) : (String) value;
                String displayText = display == null ? "" : display;
                if (!control.getText().equals(displayText)) {
                    control.setText(displayText);
                }
                decoration.hide();
            }, () -> {
                String value = control.getText();
                IStatus status = toModelValidator != null ? toModelValidator.apply(value) : UIValidator.ok();
                if (!status.isOK()) {
                    decoration.setDescriptionText(status.getMessage());
                    decoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(
                        status.getSeverity() == IStatus.WARNING ? FieldDecorationRegistry.DEC_WARNING : FieldDecorationRegistry.DEC_ERROR
                    ).getImage());
                    decoration.show();
                    return text.get();
                }
                decoration.hide();
                return toModelConverter != null ? toModelConverter.apply(value) : (T) value;
            }, SWT.Modify);
        }
    }

    static final class ButtonBuilderImpl extends UIControlBuilderImpl<ButtonBuilder, Button> implements ButtonBuilder {
        enum Kind {
            BUTTON,
            CHECK,
            RADIO;

            int toSWT() {
                return switch (this) {
                    case BUTTON -> SWT.NONE;
                    case CHECK -> SWT.CHECK;
                    case RADIO -> SWT.RADIO;
                };
            }
        }

        private final UIObservable<String> text;
        private final Consumer<SelectionEvent> onSelect;
        private final Kind kind;
        private UIObservable<Boolean> selected;

        ButtonBuilderImpl(@Nullable UIObservable<String> text, @Nullable Consumer<SelectionEvent> onSelect, @NotNull Kind kind) {
            this.text = text;
            this.onSelect = onSelect;
            this.kind = kind;
        }

        @NotNull
        @Override
        public ButtonBuilder selected(@NotNull UIObservable<Boolean> binding) {
            selected = binding;
            return this;
        }

        @NotNull
        @Override
        protected Button create(@NotNull BindingContext context, @NotNull Composite parent) {
            Button button = UIControlFactory.createButton(parent, kind.toSWT());
            if (onSelect != null) {
                button.addSelectionListener(SelectionListener.widgetSelectedAdapter(onSelect));
            }
            return button;
        }

        @Nullable
        @Override
        protected Point preferredSize(@NotNull Button control) {
            if (kind == Kind.BUTTON) {
                return new Point(UIUtils.getDialogButtonWidth(control), SWT.DEFAULT);
            } else {
                return null;
            }
        }

        @Override
        protected void bind(@NotNull BindingContext context, @NotNull Button control, @Nullable UIRowBuilderImpl row) {
            super.bind(context, control, row);
            if (text != null) {
                context.bind(control, delegate(text), value -> control.setText(value == null ? "" : value), null, SWT.NONE);
            }
            if (selected != null) {
                context.bind(control, delegate(selected), control::setSelection, control::getSelection, SWT.Selection);
            }
        }
    }

    static final class ComboBuilderImpl<T> extends UIControlBuilderImpl<ComboBuilder<T>, Combo> implements ComboBuilder<T> {
        private final UIObservableList<? extends T> items;
        private final UIObservable<T> binding;
        private final Function<? super T, String> converter;
        private final int style;

        public ComboBuilderImpl(
            @NotNull UIObservableList<? extends T> items,
            @NotNull UIObservable<T> binding,
            @NotNull Function<? super T, String> converter,
            int style
        ) {
            this.items = items;
            this.binding = binding;
            this.converter = converter;
            this.style = style;
        }

        @NotNull
        @Override
        protected Combo create(@NotNull BindingContext context, @NotNull Composite parent) {
            Combo combo = UIControlFactory.createCombo(parent, style);
            for (T item : items) {
                combo.add(converter.apply(item));
            }
            return combo;
        }

        @Override
        protected void bind(@NotNull BindingContext context, @NotNull Combo control, @Nullable UIRowBuilderImpl row) {
            super.bind(context, control, row);

            context.bind(control, delegate(binding), value -> select(control, items.indexOf(value)),
                () -> control.getSelectionIndex() < 0 || control.getSelectionIndex() >= items.size()
                    ? null : items.get(control.getSelectionIndex()), SWT.Selection);
            Runnable listener = () -> {
                control.removeAll();
                for (T item : items) {
                    control.add(converter.apply(item));
                }
                // Refresh the observable index as well, without clearing the model for a temporarily empty list
                select(control, items.indexOf(binding.get()));
            };
            var observableItems = (UIObservableListImpl<?>) items;
            observableItems.addListener(listener);
            control.addDisposeListener(event -> observableItems.removeListener(listener));
        }

        private static void select(@NotNull Combo control, int index) {
            if (index < 0) {
                control.deselectAll();
            } else {
                control.select(index);
            }
        }
    }

    static final class ControlBuilderImpl extends UIControlBuilderImpl<ControlBuilder, Control> implements ControlBuilder {
        private final Function<Composite, Control> factory;

        ControlBuilderImpl(@NotNull Function<Composite, Control> factory) {
            this.factory = factory;
        }

        @NotNull
        @Override
        protected Control create(@NotNull BindingContext context, @NotNull Composite parent) {
            return factory.apply(parent);
        }
    }
}
