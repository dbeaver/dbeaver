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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.utils.CommonUtils;

public final class ConnectionLabelUtils {

    public static final String CONNECTION_SEPARATOR = " \u2014 "; //$NON-NLS-1$

    private ConnectionLabelUtils() {
    }

    public static void applyConnectionBackground(@NotNull ViewerCell cell, @NotNull DBPDataSourceContainer container) {
        Color connectionColor = UIUtils.getConnectionColor(container.getConnectionConfiguration());
        if (connectionColor != null) {
            cell.setBackground(UIStyles.mix(connectionColor, cell.getControl().getBackground(), 0.35f));
        }
    }

    public static void applyQualifierSuffix(@NotNull ViewerCell cell, @NotNull DBPDataSourceContainer container) {
        String name = container.getName();
        if (CommonUtils.isEmpty(name)) {
            return;
        }

        Color qualifierColor = JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
        if (qualifierColor == null) {
            return;
        }

        String suffix = CONNECTION_SEPARATOR + name;
        String text = cell.getText();
        int suffixStart = text.length() - suffix.length();
        if (suffixStart <= 0) {
            return;
        }

        StyleRange qualifierRange = new StyleRange(suffixStart, suffix.length(), qualifierColor, null);
        StyleRange[] existing = cell.getStyleRanges();

        if (existing == null || existing.length == 0) {
            cell.setStyleRanges(new StyleRange[]{qualifierRange});
            return;
        }

        // truncate any that bleed into the suffix region, and append the qualifier range.
        StyleRange[] merged = new StyleRange[existing.length + 1];
        int count = 0;
        for (StyleRange range : existing) {
            if (range.start + range.length <= suffixStart) {
                merged[count++] = range;
            } else if (range.start < suffixStart) {
                StyleRange truncated = (StyleRange) range.clone();
                truncated.length = suffixStart - truncated.start;
                merged[count++] = truncated;
            }
            // Ranges entirely within the suffix region are replaced by qualifierRange
        }
        merged[count++] = qualifierRange;

        if (count < merged.length) {
            StyleRange[] result = new StyleRange[count];
            System.arraycopy(merged, 0, result, 0, count);
            cell.setStyleRanges(result);
        } else {
            cell.setStyleRanges(merged);
        }
    }
}
