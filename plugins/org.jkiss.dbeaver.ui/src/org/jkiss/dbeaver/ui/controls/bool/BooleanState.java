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
package org.jkiss.dbeaver.ui.controls.bool;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.internal.UIMessages;

public enum BooleanState {
    CHECKED("checked", UIMessages.control_boolean_state_checked, UIIcon.CHECK_ON, new String[]{"☑", "[v]", "yes", String.valueOf(true)}),
    UNCHECKED("unchecked", UIMessages.control_boolean_state_unchecked, UIIcon.CHECK_OFF, new String[]{"☐", "[ ]", "no", String.valueOf(false)}),
    NULL("null", UIMessages.control_boolean_state_null, UIIcon.CHECK_QUEST, new String[]{"☒", "[?]", DBConstants.NULL_VALUE_LABEL});

    private final String id;
    private final String label;
    private final DBIcon icon;
    private final String[] predefinedTextStyles;

    BooleanState(@NotNull String id, @NotNull String label, @NotNull DBIcon icon, @NotNull String[] predefinedTextStyles) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.predefinedTextStyles = predefinedTextStyles;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    @NotNull
    public DBIcon getIcon() {
        return icon;
    }

    @NotNull
    public String[] getPredefinedTextStyles() {
        return predefinedTextStyles;
    }

    @NotNull
    public <T> T choose(@NotNull T checked, @NotNull T unchecked, @NotNull T none) {
        return this == CHECKED ? checked : this == UNCHECKED ? unchecked : none;
    }
}
