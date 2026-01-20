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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class UIPanelBuilderImpl extends UIControlBuilderImpl<UIPanelBuilder, Control> implements UIPanelBuilder {
    private final List<UIRowBuilderImpl> rows = new ArrayList<>();
    private final String text;
    private final boolean expandable;
    private final boolean expanded;
    private int indent = 0;
    private int marginLeft = 5;
    private int marginTop = 5;
    private int marginRight = 5;
    private int marginBottom = 5;

    private UIPanelBuilderImpl(@Nullable String text, boolean expandable, boolean expanded) {
        this.text = text;
        this.expandable = expandable;
        this.expanded = expanded;
    }

    @NotNull
    static UIPanelBuilderImpl panel() {
        return new UIPanelBuilderImpl(null, false, false);
    }

    @NotNull
    static UIPanelBuilderImpl group(@NotNull String text) {
        return new UIPanelBuilderImpl(text, false, false);
    }

    @NotNull
    static UIPanelBuilderImpl expandableGroup(@NotNull String text, boolean expanded) {
        return new UIPanelBuilderImpl(text, true, expanded);
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

        Composite host;
        if (expandable) {
            host = UIControlFactory.createExpandableComposite(parent);
        } else {
            host = parent;
        }

        Composite client;
        if (text != null && !expandable) {
            client = UIControlFactory.createTitledComposite(host, text);
        } else {
            client = UIControlFactory.createComposite(host);
        }

        // Compute max number of columns based on rows' controls
        int columns = rows.stream()
            .mapToInt(row -> row.controls.size())
            .max().orElseThrow();

        GridLayoutFactory.fillDefaults()
            .numColumns(columns)
            .extendedMargins(marginLeft, marginRight, marginTop, marginBottom)
            .applyTo(client);

        for (UIRowBuilderImpl row : rows) {
            buildRow(context, row, client, columns);
        }

        if (expandable) {
            var composite = (ExpandableComposite) host;
            composite.setClient(client);
            composite.setText(text);
            composite.setExpanded(expanded, true);
            return composite;
        }

        return client;
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
