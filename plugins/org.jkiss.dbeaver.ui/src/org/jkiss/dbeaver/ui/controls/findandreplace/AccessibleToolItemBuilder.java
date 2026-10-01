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
package org.jkiss.dbeaver.ui.controls.findandreplace;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.ToolItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Builder for ToolItems for {@link AccessibleToolBar}./**
 * <p>
 * Derived from <a href="https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/bundles/org.eclipse.ui.workbench.texteditor/src/org/eclipse/ui/internal/findandreplace/overlay/AccessibleToolItemBuilder.java">eclipse.platform.ui</a>
 */
public class AccessibleToolItemBuilder {
    @NotNull
    private final AccessibleToolBar accessibleToolBar;

    private int styleBits = SWT.NONE;

    @Nullable
    private Image image = null;

    @Nullable
    private String toolTipText = null;

    @NotNull
    private List<KeyStroke> shortcuts = Collections.emptyList();

    @Nullable
    private Runnable operation = null;

    @Nullable
    private MenuManager contextMenuManager = null;

    public AccessibleToolItemBuilder(@NotNull AccessibleToolBar accessibleToolBar) {
        this.accessibleToolBar = Objects.requireNonNull(accessibleToolBar);
    }

    @NotNull
    public AccessibleToolItemBuilder withStyleBits(int newStyleBits) {
        this.styleBits = newStyleBits;
        return this;
    }

    @NotNull
    public AccessibleToolItemBuilder withImage(@Nullable Image newImage) {
        this.image = newImage;
        return this;
    }

    @NotNull
    public AccessibleToolItemBuilder withToolTipText(@Nullable String newToolTipText) {
        this.toolTipText = newToolTipText;
        return this;
    }

    @NotNull
    public AccessibleToolItemBuilder withShortcuts(@NotNull List<KeyStroke> newShortcuts) {
        this.shortcuts = newShortcuts;
        return this;
    }

    @NotNull
    public AccessibleToolItemBuilder withOperation(@NotNull Runnable newOperation) {
        this.operation = newOperation;
        return this;
    }

    @NotNull
    public AccessibleToolItemBuilder withContextMenu(@NotNull MenuManager contextMenuManager) {
        this.contextMenuManager = contextMenuManager;
        return this;
    }

    @NotNull
    public ToolItem build() {
        AccessibleToolItem accessibleToolItem = this.accessibleToolBar.createToolItem(this.styleBits);
        if (this.image != null) {
            accessibleToolItem.setImage(this.image);
        }
        if (this.toolTipText != null) {
            accessibleToolItem.setToolTipText(this.toolTipText);
        }
        if (this.operation != null) {
            accessibleToolItem.setOperation(this.operation, this.shortcuts);
        }
        if (this.contextMenuManager != null) {
            accessibleToolItem.setContextMenuManager(this.contextMenuManager);
        }

        return accessibleToolItem.getToolItem();
    }
}
