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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;

import java.util.function.Consumer;

/**
 * The builder for a panel.
 */
public sealed interface UIPanelBuilder extends UIControlBuilder<UIPanelBuilder> permits UIPanelBuilderImpl {
    @NotNull
    static Control build(@NotNull Composite parent, @NotNull Consumer<? super UIPanelBuilder> handler) {
        var builder = UIPanelBuilderImpl.panel();
        handler.accept(builder);
        return builder.build(new DataBindingContext(), parent, null);
    }

    @NotNull
    UIPanelBuilder margins(int horizontal, int vertical);

    @NotNull
    UIPanelBuilder margins(int left, int top, int right, int bottom);

    @NotNull
    UIPanelBuilder row(@NotNull Consumer<? super UIRowBuilder> handler);

    @NotNull
    default UIPanelBuilder row(@NotNull String label, @NotNull Consumer<? super UIRowBuilder> handler) {
        return row(rb -> handler.accept(rb.label(label + ":")));
    }

    @NotNull
    UIPanelBuilder indent(@NotNull Consumer<? super UIPanelBuilder> handler);
}
