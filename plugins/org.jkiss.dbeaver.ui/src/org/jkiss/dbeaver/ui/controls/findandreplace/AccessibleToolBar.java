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

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * This class wraps the ToolBar to make it possible to use tabulator-keys to
 * navigate between the buttons of a ToolBar. For this, we simulate a singular
 * ToolBar by putting each ToolItem into it's own ToolBar and composing them
 * into a Composite. Since the "Enter" keypress could not previously trigger
 * activation behavior, we listen for it manually and send according events if
 * necessary.
 * <p>
 * Derived from <a href="https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/bundles/org.eclipse.ui.workbench.texteditor/src/org/eclipse/ui/internal/findandreplace/overlay/AccessibleToolBar.java">eclipse.platform.ui</a>
 */
public class AccessibleToolBar extends Composite {

    @NotNull
    private final List<AccessibleToolItem> accessibleToolItems = new ArrayList<>();

    @NotNull
    private final GridLayout layout;

    public AccessibleToolBar(@NotNull Composite parent) {
        super(parent, SWT.NONE);
        this.layout = GridLayoutFactory.fillDefaults().numColumns(0).spacing(0, 0).margins(1, 1).create();
        this.setLayout(layout);
    }

    /**
     * Creates a ToolItem handled by this ToolBar and returns it. Will add a
     * KeyListener which will handle presses of "Enter".
     *
     * @param styleBits the StyleBits to apply to the created ToolItem
     * @return a newly created ToolItem
     */
    @NotNull
    public AccessibleToolItem createToolItem(int styleBits) {
        AccessibleToolItem toolItem = new AccessibleToolItem(this, styleBits);
        this.accessibleToolItems.add(toolItem);
        this.layout.numColumns++;
        return toolItem;
    }

    @Override
    public void setBackground(@Nullable Color color) {
        super.setBackground(color);
        // some ToolItems (like SWT.SEPARATOR) don't easily inherit the color from the
        // parent control
        for (AccessibleToolItem item : this.accessibleToolItems) {
            item.setBackground(color);
        }
    }

    void registerActionShortcutsAtControl(@NotNull Control control) {
        for (AccessibleToolItem item : this.accessibleToolItems) {
            item.registerActionShortcutsAtControl(control);
        }
    }

    @Nullable
    Control getFirstControl() {
        Control[] children = this.getChildren();
        return children.length == 0 ? null : children[0];
    }

    public void forceFirstControlFocus() {
        Control control = this.getFirstControl();
        if (control != null) {
            control.forceFocus();
        }
    }
}
