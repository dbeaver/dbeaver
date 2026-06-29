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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.List;

/**
 * Derived from <a href="https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/bundles/org.eclipse.ui.workbench.texteditor/src/org/eclipse/ui/internal/findandreplace/overlay/AccessibleToolItem.java">eclipse.platform.ui</a>
 */
public class AccessibleToolItem {
    @NotNull
    private final ToolBar toolbar;
    @NotNull
    private final ToolItem toolItem;

    @NotNull
    private FindReplaceOverlayAction action = new FindReplaceOverlayAction(() -> { });

    AccessibleToolItem(@NotNull Composite parent, int styleBits) {
        this.toolbar = new ToolBar(parent, SWT.FLAT | SWT.HORIZONTAL);
        GridDataFactory.fillDefaults().grab(true, true).align(SWT.CENTER, SWT.CENTER).applyTo(this.toolbar);
        this.toolItem = new ToolItem(this.toolbar, styleBits);
        this.addToolItemTraverseListener(this.toolbar);
    }

    private void addToolItemTraverseListener(@NotNull ToolBar parent) {
        parent.addTraverseListener(e -> {
            if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                this.action.execute();
                e.doit = false;
            }
        });
    }

    @NotNull
    ToolItem getToolItem() {
        return this.toolItem;
    }

    void setBackground(@Nullable Color color) {
        this.toolItem.getParent().setBackground(color);
    }

    void setImage(@NotNull Image image) {
        this.toolItem.setImage(image);
    }

    void setToolTipText(@NotNull String text) {
        this.toolItem.setToolTipText(action.addShortcutHintToTooltipText(text));
    }

    void setOperation(@NotNull Runnable operation, @NotNull List<KeyStroke> shortcuts) {
        boolean isCheckbox = (this.toolItem.getStyle() & SWT.CHECK) != 0;
        if (isCheckbox) {
            this.action = new FindReplaceOverlayAction(() -> {
                this.toolItem.setSelection(!this.toolItem.getSelection());
                operation.run();
            });
        } else {
            this.action = new FindReplaceOverlayAction(operation);
        }
        this.action.addShortcuts(shortcuts);
        this.setToolTipText(this.toolItem.getToolTipText());
        this.toolItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> operation.run()));
    }

    void setContextMenuManager(@NotNull MenuManager contextMenuManager) {
        this.toolbar.addMouseListener(MouseListener.mouseDownAdapter(e -> {
            if (e.button == 3) {
                Rectangle bounds = this.toolItem.getBounds();
                Point location = this.toolbar.toDisplay(bounds.x, bounds.y);
                location.y += bounds.height;

                Menu menu = contextMenuManager.createContextMenu(this.toolbar);
                menu.setLocation(location);
                menu.setVisible(true);
            }
        }));
    }

    void registerActionShortcutsAtControl(@NotNull Control control) {
        FindReplaceShortcutUtil.registerActionShortcutsAtControl(this.action, control);
    }

}
