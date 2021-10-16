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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.internal.UIMessages;

public enum UIElementFontStyle {
    NORMAL(UIMessages.control_font_normal),
    ITALIC(UIMessages.control_font_italic),
    BOLD(UIMessages.control_font_bold);

    private final String label;

    UIElementFontStyle(@NotNull String label) {
        this.label = label;
    }

    @NotNull
    public String getLabel() {
        return label;
    }
}
