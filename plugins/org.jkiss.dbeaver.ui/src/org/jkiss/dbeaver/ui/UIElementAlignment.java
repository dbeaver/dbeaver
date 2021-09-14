/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.SWT;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.internal.UIMessages;

public enum UIElementAlignment {
    LEFT(UIMessages.control_alignment_left, SWT.LEFT, UIIcon.ALIGN_TO_LEFT),
    CENTER(UIMessages.control_alignment_center, SWT.CENTER, UIIcon.ALIGN_TO_CENTER),
    RIGHT(UIMessages.control_alignment_right, SWT.RIGHT, UIIcon.ALIGN_TO_RIGHT);

    private final String label;
    private final int style;
    private final DBIcon icon;

    UIElementAlignment(@NotNull String label, int style, @NotNull DBIcon icon) {
        this.label = label;
        this.style = style;
        this.icon = icon;
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    public int getStyle() {
        return style;
    }

    @NotNull
    public DBIcon getIcon() {
        return icon;
    }
}
