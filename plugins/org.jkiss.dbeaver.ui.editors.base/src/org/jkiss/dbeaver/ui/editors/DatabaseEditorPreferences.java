/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

/**
 * DatabaseEditorPreferences
 */
public class DatabaseEditorPreferences {
    public enum BreadcrumbLocation {
        IN_STATUS_BAR,
        IN_EDITORS,
        HIDDEN;

        @NotNull
        public static BreadcrumbLocation get(@NotNull DBPPreferenceStore store) {
            return get(store.getString(UI_STATUS_BAR_SHOW_BREADCRUMBS));
        }

        @NotNull
        public static BreadcrumbLocation getDefault(@NotNull DBPPreferenceStore store) {
            return get(store.getDefaultString(UI_STATUS_BAR_SHOW_BREADCRUMBS));
        }

        @NotNull
        private static BreadcrumbLocation get(@Nullable String value) {
            return switch (CommonUtils.notEmpty(value)) {
                case "HIDDEN", "false" -> HIDDEN;
                case "IN_EDITORS" -> IN_EDITORS;
                default -> IN_STATUS_BAR;
            };
        }
    }

    public static final String PROP_TITLE_SHOW_FULL_NAME = "navigator.editor.full-name"; //$NON-NLS-1$
    public static final String PROP_SAVE_EDITORS_STATE = "ui.editors.reopen-after-restart"; //$NON-NLS-1$
    public static final String PROP_KEEP_EDITORS_ON_DISCONNECT = "ui.editors.keep-editors-on-disconnect"; //$NON-NLS-1$
    public static final String PROP_DISCONNECT_ON_EDITORS_CLOSE = "ui.editors.disconnect-on-editors-close"; //$NON-NLS-1$
    public static final String UI_STATUS_BAR_SHOW_BREADCRUMBS = "ui.statusBar.show.breadcrumbs"; //$NON-NLS-1$
}
