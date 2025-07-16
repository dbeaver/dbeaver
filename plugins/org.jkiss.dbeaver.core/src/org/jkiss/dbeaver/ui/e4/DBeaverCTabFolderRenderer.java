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

import org.eclipse.e4.ui.internal.workbench.swt.AbstractPartRenderer;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceContainerProvider;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;

import java.lang.reflect.Field;

public final class DBeaverCTabFolderRenderer extends CTabRendering {
    private static final Log log = Log.getLog(DBeaverCTabFolderRenderer.class);

    private static final Rectangle EMPTY_CLOSE_RECT = new Rectangle(0, 0, 0, 0);

    private static final FieldReflection<CTabRendering, Color> selectedTabHighlightColorField;
    private static final FieldReflection<CTabRendering, Color> hotUnselectedTabsColorBackgroundField;
    private static final FieldReflection<CTabItem, Integer> closeImageStateField;
    private static final FieldReflection<CTabItem, Rectangle> closeRectField;

    static {
        selectedTabHighlightColorField = FieldReflection.of(CTabRendering.class, "selectedTabHighlightColor");
        hotUnselectedTabsColorBackgroundField = FieldReflection.of(CTabRendering.class, "hotUnselectedTabsColorBackground");
        closeImageStateField = FieldReflection.of(CTabItem.class, "closeImageState");
        closeRectField = FieldReflection.of(CTabItem.class, "closeRect");
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
                var oldHotUnselectedTabsColorBackground = hotUnselectedTabsColorBackgroundField.get(this);
                var oldSelectedTabHighlightColor = selectedTabHighlightColorField.get(this);
                var oldCloseRect = closeRectField.get(item);
                var oldCloseImageState = closeImageStateField.get(item);

                // Removes the background behind the close button
                if (oldCloseImageState != null && oldCloseImageState == SWT.BACKGROUND) {
                    closeRectField.set(item, EMPTY_CLOSE_RECT);
                }

                // Replaces unselected and selected tab colors
                boolean paintingHotTab = (state & SWT.HOT) != 0;
                hotUnselectedTabsColorBackgroundField.set(this, paintingHotTab ? UIStyles.lighten(color, 0.1f) : color);
                selectedTabHighlightColorField.set(this, color);

                super.draw(part, state | SWT.HOT, bounds, gc);

                // Restore whatever we have changed back to original values
                closeRectField.set(item, oldCloseRect);
                selectedTabHighlightColorField.set(this, oldSelectedTabHighlightColor);
                hotUnselectedTabsColorBackgroundField.set(this, oldHotUnselectedTabsColorBackground);

                return;
            }
        }

        super.draw(part, state, bounds, gc);
    }

    @Nullable
    private static Color getConnectionColor(@NotNull CTabItem item) {
        if (!(item.getData(AbstractPartRenderer.OWNING_ME) instanceof MPart part)) {
            return null;
        }

        return getConnectionColor(part);
    }

    @Nullable
    private static Color getConnectionColor(@NotNull MPart part) {
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
            } catch (PartInitException e) {
                log.debug("Cannot get editor input for part: " + part.getElementId(), e);
            }
        }

        return null;
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
        if (editorInput instanceof IDatabaseEditorInput databaseEditorInput) {
            return databaseEditorInput.getConnectionColor();
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
