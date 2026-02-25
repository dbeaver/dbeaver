/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

import java.util.function.Function;

/**
 * Label provider for the editor dropdown (Ctrl+E) that shows connection color
 * and optional connection name so tabs with the same title from different
 * connections can be distinguished.
 */
class EditorDropdownLabelProvider extends SearchCellLabelProvider {

    private static final RGB BLACK = new RGB(0, 0, 0);
    private static final RGB WHITE = new RGB(255, 255, 255);
    private static final int BLEND_RATIO = 15;

    private final String pattern;
    private final Function<WorkbenchPartReference, String> baseTextSupplier;

    public EditorDropdownLabelProvider(
        @Nullable String pattern,
        @NotNull Function<WorkbenchPartReference, String> baseTextSupplier
    ) {
        this.pattern = pattern;
        this.baseTextSupplier = baseTextSupplier;
    }

    @Override
    public void update(@NotNull ViewerCell cell) {
        super.update(cell);
        Object element = cell.getElement();
        if (!(element instanceof IEditorReference ref)) {
            return;
        }
        Color bg = getConnectionBackground(ref);
        cell.setBackground(bg);
    }

    @NotNull
    @Override
    public String getText(@NotNull Object element) {
        String base = baseTextSupplier.apply((WorkbenchPartReference) element);
        if (!(element instanceof IEditorReference ref)) {
            return base != null ? base : "";
        }
        return getDisplayText(base, ref);
    }

    /**
     * Builds the display text for the dropdown (base title + connection name when present).
     * Used by both the label provider and the filter so behavior stays in sync.
     */
    @NotNull
    public static String getDisplayText(@Nullable String baseText, @NotNull IEditorReference ref) {
        String base = baseText != null ? baseText : "";
        String conn = getConnectionNameForReference(ref);
        if (CommonUtils.isEmpty(conn)) {
            return base;
        }
        return base + " (" + conn + ")";
    }

    @NotNull
    @Override
    public Image getImage(@NotNull Object element) {
        return ((WorkbenchPartReference) element).getTitleImage();
    }

    @Override
    public String getToolTipText(@NotNull Object element) {
        return ((WorkbenchPartReference) element).getTitleToolTip();
    }

    @Nullable
    @Override
    public String getPattern() {
        return pattern;
    }

    /**
     * Returns the connection name for the given editor reference if the input
     * supports it. Used by the dropdown filter so search matches the displayed text.
     */
    @Nullable
    public static String getConnectionNameForReference(@NotNull IEditorReference ref) {
        try {
            IEditorInput input = ref.getEditorInput();
            if (input instanceof IEditorConnectionColorProvider provider) {
                return provider.getConnectionName();
            }
        } catch (Exception e) {
            // Editor not restored or input not available
        }
        return null;
    }

    @Nullable
    private static Color getConnectionBackground(@NotNull IEditorReference ref) {
        try {
            IEditorInput input = ref.getEditorInput();
            if (!(input instanceof IEditorConnectionColorProvider provider)) {
                return null;
            }
            Color connectionColor = provider.getConnectionColor();
            if (connectionColor == null) {
                return null;
            }
            Color listBackground = UIStyles.getDefaultTextBackground();
            SharedTextColors sharedColors = UIUtils.getSharedTextColors();
            boolean listIsDark = UIUtils.isDark(listBackground.getRGB());
            RGB blended = UIUtils.blend(
                listIsDark ? BLACK : WHITE,
                connectionColor.getRGB(),
                BLEND_RATIO
            );
            return sharedColors.getColor(blended);
        } catch (Exception e) {
            return null;
        }
    }
}
