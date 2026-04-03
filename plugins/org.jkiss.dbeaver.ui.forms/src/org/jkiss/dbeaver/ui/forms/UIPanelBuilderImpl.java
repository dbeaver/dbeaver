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
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TitledComposite;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class UIPanelBuilderImpl extends UIControlBuilderImpl<UIPanelBuilder, Control> implements UIPanelBuilder {
    sealed interface Kind {
        record Expandable(@NotNull String text, boolean expanded) implements Kind {
        }

        record Titled(@NotNull String text) implements Kind {
        }

        record Scrolled(boolean horizontal, boolean vertical) implements Kind {
        }

        record Simple() implements Kind {
        }
    }

    private final Kind kind;
    private final List<UIRowBuilderImpl> rows = new ArrayList<>();
    private int indent = 0;
    private int marginLeft = 0;
    private int marginTop = 0;
    private int marginRight = 0;
    private int marginBottom = 0;

    private UIPanelBuilderImpl(@NotNull Kind kind) {
        this.kind = kind;
    }

    @NotNull
    static UIPanelBuilderImpl panel() {
        return new UIPanelBuilderImpl(new Kind.Simple());
    }

    @NotNull
    static UIPanelBuilderImpl expandable(@NotNull String text, boolean expanded) {
        return new UIPanelBuilderImpl(new Kind.Expandable(text, expanded));
    }

    @NotNull
    static UIPanelBuilderImpl titled(@NotNull String text) {
        return new UIPanelBuilderImpl(new Kind.Titled(text));
    }

    @NotNull
    static UIPanelBuilderImpl scrolled(boolean horizontal, boolean vertical) {
        return new UIPanelBuilderImpl(new Kind.Scrolled(horizontal, vertical));
    }

    @NotNull
    @Override
    public UIPanelBuilder margins(int horizontal, int vertical) {
        marginLeft = marginRight = horizontal;
        marginTop = marginBottom = vertical;
        return this;
    }

    @NotNull
    @Override
    public UIPanelBuilder margins(int left, int top, int right, int bottom) {
        marginLeft = left;
        marginTop = top;
        marginRight = right;
        marginBottom = bottom;
        return this;
    }

    @NotNull
    @Override
    public UIPanelBuilder row(@NotNull Consumer<? super UIRowBuilder> handler) {
        var builder = new UIRowBuilderImpl(indent);
        handler.accept(builder);
        if (builder.controls.isEmpty()) {
            throw new IllegalStateException("Row cannot be empty");
        }
        rows.add(builder);
        return this;
    }

    @NotNull
    @Override
    public UIPanelBuilder indent(@NotNull Consumer<? super UIPanelBuilder> handler) {
        indent++;
        handler.accept(this);
        indent--;
        return this;
    }

    @NotNull
    @Override
    protected Control create(@NotNull DataBindingContext context, @NotNull Composite parent) {
        if (rows.isEmpty()) {
            throw new IllegalStateException("Panel cannot be empty");
        }

        Composite host = switch (kind) {
            case Kind.Expandable k -> UIControlFactory.createExpandableComposite(parent, k.text());
            case Kind.Titled k -> UIControlFactory.createTitledComposite(parent, k.text());
            case Kind.Scrolled k -> UIControlFactory.createScrolledComposite(parent, k.horizontal(), k.vertical());
            case Kind.Simple ignored -> parent;
        };
        Composite client = UIControlFactory.createComposite(host);

        // Compute max number of columns based on rows' controls
        int columns = rows.stream()
            .mapToInt(row -> row.controls.size())
            .max().orElseThrow();

        GridLayoutFactory.fillDefaults()
            .numColumns(columns)
            .margins(0, 0)
            .extendedMargins(marginLeft, marginRight, marginTop, marginBottom)
            .applyTo(client);

        for (UIRowBuilderImpl row : rows) {
            buildRow(context, row, client, columns);
        }

        return switch (kind) {
            case Kind.Expandable k -> {
                var composite = (ExpandableComposite) host;
                composite.setClient(client);
                composite.setExpanded(k.expanded(), true);
                yield composite;
            }
            case Kind.Titled ignored -> {
                var composite = (TitledComposite) host;
                composite.setClient(client);
                yield composite;
            }
            case Kind.Scrolled ignored -> {
                var composite = (ScrolledComposite) host;
                UIUtils.configureScrolledComposite(composite, client);
                yield composite;
            }
            case Kind.Simple ignored -> client;
        };
    }

    private void buildRow(
        @NotNull DataBindingContext context,
        @NotNull UIRowBuilderImpl row,
        @NotNull Composite parent,
        int columns
    ) {
        for (int i = 0; i < row.controls.size(); i++) {
            @SuppressWarnings("unchecked")
            var builder = (UIControlBuilderImpl<?, Control>) row.controls.get(i);

            var data = new GridData();
            data.horizontalAlignment = builder.alignX;
            data.verticalAlignment = builder.alignY;
            data.grabExcessHorizontalSpace = builder.grow;
            data.grabExcessVerticalSpace = builder.grow;

            var control = builder.build(context, parent, row);

            if (builder.widthHint != SWT.DEFAULT && builder.heightHint != SWT.DEFAULT) {
                data.widthHint = builder.widthHint;
                data.heightHint = builder.heightHint;
            } else {
                var size = builder.preferredSize(control);
                if (size != null) {
                    data.widthHint = size.x;
                    data.heightHint = size.y;
                }
            }

            // Indent the first control if needed
            if (i == 0 && row.indent > 0) {
                data.horizontalIndent = LayoutConstants.getIndent() * row.indent;
            }

            // Stretch the last control to span the remaining columns
            if (i == row.controls.size() - 1) {
                data.horizontalSpan = columns - row.controls.size() + 1;
            }

            control.setLayoutData(data);
        }
    }
}
