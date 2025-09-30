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
package org.jkiss.dbeaver.ui.e4;

import org.eclipse.e4.ui.internal.css.swt.ICTabRendering;
import org.eclipse.e4.ui.internal.workbench.swt.AbstractPartRenderer;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderRenderer;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceContainerProvider;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.Field;

public final class DBeaverCTabFolderRenderer extends CTabRendering implements ICTabRendering {
    private static final Log log = Log.getLog(DBeaverCTabFolderRenderer.class);

    private static final Rectangle EMPTY_CLOSE_RECT = new Rectangle(0, 0, 0, 0);
    private static final String PART_SKIP_KEY = DBeaverCTabFolderRenderer.class.getName() + ".skipPart";

    private static final FieldReflection<CTabRendering, Color> tabOutlineColorField;
    private static final FieldReflection<CTabRendering, Color> selectedTabHighlightColorField;
    private static final FieldReflection<CTabRendering, Color[]> selectedTabFillColorsField;
    private static final FieldReflection<CTabRendering, Color> hotUnselectedTabsColorBackgroundField;
    private static final FieldReflection<CTabItem, Integer> closeImageStateField;
    private static final FieldReflection<CTabItem, Rectangle> closeRectField;
    private static final FieldReflection<CTabFolderRenderer, Integer> curveWidth;
    private static final FieldReflection<CTabFolderRenderer, Integer> curveIndent;
    private static volatile boolean isInColor;

    static {
        tabOutlineColorField = FieldReflection.of(CTabRendering.class, "tabOutlineColor");
        selectedTabHighlightColorField = FieldReflection.of(CTabRendering.class, "selectedTabHighlightColor");
        selectedTabFillColorsField = FieldReflection.of(CTabRendering.class, "selectedTabFillColors");
        hotUnselectedTabsColorBackgroundField = FieldReflection.of(CTabRendering.class, "hotUnselectedTabsColorBackground");
        closeImageStateField = FieldReflection.of(CTabItem.class, "closeImageState");
        closeRectField = FieldReflection.of(CTabItem.class, "closeRect");
        curveWidth = FieldReflection.of(CTabFolderRenderer.class, "curveWidth");
        curveIndent = FieldReflection.of(CTabFolderRenderer.class, "curveIndent");
    }

    public DBeaverCTabFolderRenderer(@NotNull CTabFolder parent) {
        super(parent);
    }

    @Override
    protected void draw(int part, int state, Rectangle bounds, GC gc) {
        if (part >= 0 && part < parent.getItemCount()) {
            CTabItem item = parent.getItem(part);
            Color color = getConnectionColor(item);

            if (color != null) {
                var oldTabOutlineColor = tabOutlineColorField.get(this);
                var oldHotUnselectedTabsColorBackground = hotUnselectedTabsColorBackgroundField.get(this);
                var oldSelectedTabHighlightColor = selectedTabHighlightColorField.get(this);
                var oldSelectedTabFillColors = selectedTabFillColorsField.get(this);
                var oldCloseRect = closeRectField.get(item);
                var oldCloseImageState = closeImageStateField.get(item);

                // Removes the background behind the close button
                if (oldCloseImageState != null && oldCloseImageState == SWT.BACKGROUND) {
                    closeRectField.set(item, EMPTY_CLOSE_RECT);
                }

                // Replaces unselected and selected tab colors
                boolean isHot = (state & SWT.HOT) != 0;
                boolean isSelected = (state & SWT.SELECTED) != 0;
                boolean isDarkTheme = UIStyles.isDarkTheme();

                Color fillColor = oldSelectedTabFillColors != null && oldSelectedTabFillColors.length == 1
                    ? oldSelectedTabFillColors[0]
                    : parent.getSelectionBackground();
                Color highlightColor = isDarkTheme ? UIStyles.lighten(color, 0.2f) : UIStyles.darken(color, 0.2f);
                Color selectedColor = UIStyles.mix(highlightColor, fillColor, 0.1f);

                hotUnselectedTabsColorBackgroundField.set(this, isHot ? selectedColor : color);
                selectedTabFillColorsField.set(this, new Color[]{selectedColor});
                selectedTabHighlightColorField.set(this, highlightColor);

                if (!isSelected) {
                    // The outline bleeds over the hover tab. Since we're relying on SWT.HOT painting
                    // logic, we need to override it to be the same color as the tab itself
                    tabOutlineColorField.set(this, isHot ? selectedColor : color);
                }

                super.draw(part, state | SWT.HOT, bounds, gc);

                // Restore whatever we have changed back to original values
                closeRectField.set(item, oldCloseRect);
                selectedTabHighlightColorField.set(this, oldSelectedTabHighlightColor);
                selectedTabFillColorsField.set(this, oldSelectedTabFillColors);
                hotUnselectedTabsColorBackgroundField.set(this, oldHotUnselectedTabsColorBackground);
                tabOutlineColorField.set(this, oldTabOutlineColor);

                return;
            }
        }

        super.draw(part, state, bounds, gc);
    }

    @Override
    protected Rectangle computeTrim(int part, int state, int x, int y, int width, int height) {
        try {
            return super.computeTrim(part, state, x, y, width, height);
        } finally {
            resetCurves();
        }
    }

    @Override
    protected Point computeSize(int part, int state, GC gc, int wHint, int hHint) {
        try {
            return super.computeSize(part, state, gc, wHint, hHint);
        } finally {
            resetCurves();
        }
    }

    private void resetCurves() {
        if (RuntimeUtils.isLinux()) {
            // Tab rendering is broken on Linux when a different renderer other than org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering is used:
            // https://github.com/eclipse-platform/eclipse.platform.swt/blob/1a1f0c22b89d8c99ff9ad58c2bbcf82147852e5a/bundles/org.eclipse.swt/Eclipse%20SWT%20Custom%20Widgets/common/org/eclipse/swt/custom/CTabFolderRenderer.java#L1795-L1796
            // The issue can be fixed by resetting these fields:
            curveWidth.set(this, 0);
            curveIndent.set(this, 0);
        }
    }

    @Nullable
    private static Color getConnectionColor(@NotNull CTabItem item) {
        if (item.getData(AbstractPartRenderer.OWNING_ME) instanceof MPart part) {
            return getConnectionColor(part);
        }
        for (Control control = item.getParent(); control != null; control = control.getParent()) {
            if (control.getData(AbstractPartRenderer.OWNING_ME) instanceof MPart part) {
                return getConnectionColor(part);
            }
        }
        return null;
    }

    @Nullable
    private static Color getConnectionColor(@NotNull MPart part) {
        if (part.getTransientData().containsKey(PART_SKIP_KEY)) {
            return null;
        }
        if (isInColor) {
            // FIXME: this is a dirty workaround for UI freeze (dbeaver/pro#6519)
            // Freeze happens because we may trigger master password dialog in ref.getEditorInput()
            // We fix it by avoiding UI double entrance
            return null;
        }
        isInColor = true;
        try {
            if (part.getObject() instanceof CompatibilityEditor editor) {
                return getConnectionColor(editor.getEditor());
            }

            // See org.eclipse.ui.internal.WorkbenchPartReference.WorkbenchPartReference
            if (part.getTransientData().get(IWorkbenchPartReference.class.getName()) instanceof IEditorReference ref) {
                IEditorPart editor = ref.getEditor(false);
                if (editor != null) {
                    return getConnectionColor(editor);
                }

                try {
                    return getConnectionColor(ref.getEditorInput());
                } catch (Exception e) {
                    // If for whatever reason we failed to retrieve the editor input with an exception,
                    // it's likely to happen again. To avoid such scenarios, we set this key so it will
                    // cause all future calls for this part to return early.
                    part.getTransientData().put(PART_SKIP_KEY, Boolean.TRUE);
                    log.debug("Cannot get editor input for part: " + part.getElementId(), e);
                }
            }

            return null;
        } finally {
            isInColor = false;
        }
    }

    @Nullable
    private static Color getConnectionColor(@NotNull IEditorPart editorPart) {
        if (editorPart instanceof DBPDataSourceContainerProvider provider) {
            DBPDataSourceContainer container = provider.getDataSourceContainer();
            if (container != null) {
                return UIUtils.getConnectionColor(container.getConnectionConfiguration());
            }
        }

        return getConnectionColor(editorPart.getEditorInput());
    }

    @Nullable
    private static Color getConnectionColor(@NotNull IEditorInput editorInput) {
        DBPDataSourceContainer container = EditorUtils.getInputDataSource(editorInput, false);
        if (container != null) {
            return UIUtils.getConnectionColor(container.getConnectionConfiguration());
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private record FieldReflection<T_CLASS, T_FIELD>(@Nullable Field field) {
        static <T, R> FieldReflection<T, R> of(@NotNull Class<T> declaringClass, @NotNull String fieldName) {
            Field field = null;

            try {
                field = declaringClass.getDeclaredField(fieldName);
                field.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                log.error("Cannot get field '" + fieldName + "' from class " + declaringClass.getName(), e);
            }

            return new FieldReflection<>(field);
        }

        @Nullable
        T_FIELD get(@NotNull T_CLASS object) {
            if (field == null) {
                return null;
            }
            try {
                return (T_FIELD) field.get(object);
            } catch (ReflectiveOperationException e) {
                log.error("Cannot get value of field '" + field.getName() + "' from object " + object, e);
                return null;
            }
        }

        void set(@NotNull T_CLASS object, @Nullable T_FIELD value) {
            if (field == null) {
                return;
            }
            try {
                field.set(object, value);
            } catch (ReflectiveOperationException e) {
                log.error("Cannot set value of field '" + field.getName() + "' from object " + object, e);
            }
        }
    }
}
