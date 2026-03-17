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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceContainerProvider;
import org.jkiss.utils.CommonUtils;

public final class ConnectionLabelUtils {

    private static final Log log = Log.getLog(ConnectionLabelUtils.class);

    public static final String CONNECTION_SEPARATOR = " - "; //$NON-NLS-1$

    private ConnectionLabelUtils() {
    }

    /**
     * Extracts a {@link DBPDataSourceContainer} from an editor reference.
     * Tries the editor part first (if already loaded), then falls back to the editor input.
     */
    @Nullable
    public static DBPDataSourceContainer getDataSourceContainer(@NotNull IEditorReference ref) {
        try {
            IEditorPart editor = ref.getEditor(false);
            if (editor instanceof DBPDataSourceContainerProvider provider) {
                DBPDataSourceContainer container = provider.getDataSourceContainer();
                if (container != null) {
                    return container;
                }
            }
            IEditorInput input = editor != null ? editor.getEditorInput() : ref.getEditorInput();
            if (input instanceof DBPDataSourceContainerProvider provider) {
                return provider.getDataSourceContainer();
            }
            return null;
        } catch (Exception e) {
            log.debug("Cannot get editor input for: " + ref.getTitle(), e);
            return null;
        }
    }

    /**
     * Builds a label with connection name suffix appended.
     * Used by filters that need the full text for matching.
     */
    @NotNull
    public static String appendConnectionSuffix(@NotNull String label, @Nullable DBPDataSourceContainer container) {
        if (container != null && !CommonUtils.isEmpty(container.getName())) {
            return label + CONNECTION_SEPARATOR + container.getName();
        }
        return label;
    }

    /**
     * Appends the connection name suffix to the current cell text,
     * applies connection background color, and styles the suffix with qualifier color.
     * <p>
     * Must be called after {@code super.update(cell)} so that the cell text contains
     * the base label and any search-highlight StyleRanges are already set.
     * Does nothing if {@code container} is null.
     */
    public static void applyConnectionInfo(@NotNull ViewerCell cell, @Nullable DBPDataSourceContainer container) {
        if (container == null) {
            return;
        }
        String name = container.getName();
        if (CommonUtils.isEmpty(name)) {
            return;
        }

        // Append suffix - we know exactly where it starts
        String suffix = CONNECTION_SEPARATOR + name;
        String baseText = cell.getText();
        int suffixStart = baseText.length();
        cell.setText(baseText + suffix);

        // Background
        Color connectionColor = UIUtils.getConnectionColor(container.getConnectionConfiguration());
        if (connectionColor != null) {
            cell.setBackground(UIStyles.mix(connectionColor, cell.getControl().getBackground(), 0.35f));
        }

        // Qualifier styling
        Color qualifierColor = JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
        if (qualifierColor == null) {
            return;
        }

        StyleRange qualifierRange = new StyleRange(suffixStart, suffix.length(), qualifierColor, null);
        StyleRange[] existing = cell.getStyleRanges();

        if (existing == null || existing.length == 0) {
            cell.setStyleRanges(new StyleRange[]{qualifierRange});
            return;
        }

        // Existing ranges (search highlights) are within [0, suffixStart) - just append
        StyleRange[] result = new StyleRange[existing.length + 1];
        System.arraycopy(existing, 0, result, 0, existing.length);
        result[existing.length] = qualifierRange;
        cell.setStyleRanges(result);
    }
}
