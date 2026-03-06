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

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.internal.EditorReference;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.eclipse.ui.internal.WorkbookEditorsHandler;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceContainerProvider;

import java.util.HashMap;
import java.util.Map;

public class CustomWorkbookEditorsHandler extends WorkbookEditorsHandler {
    private static final Log log = Log.getLog(CustomWorkbookEditorsHandler.class);

    // FIXME: this is a dirty workaround for UI freeze (dbeaver/pro#6519)
    // Freeze happens because we may trigger master password dialog in ref.getEditorInput()
    // We fix it by avoiding UI double entrance.
    // Note: this flag is separate from DBeaverEditorPartUtils.isResolving (used by the tab
    // renderer and chevron popup). Both operate on the SWT UI thread exclusively, so
    // volatile is not needed.
    private static boolean isResolving;

    private String pattern;

    // Caches container lookups for the lifetime of a single Ctrl+E popup session.
    // Cleared each time the dialog is opened (when setLabelProvider is called)
    // and when the label provider is disposed.
    private final Map<EditorReference, DBPDataSourceContainer> containerCache = new HashMap<>();

    @Override
    protected ViewerFilter getFilter() {
        return new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return element instanceof EditorReference ref
                    && pattern != null
                    && SearchCellLabelProvider.matches(pattern, getFilterText(ref));
            }
        };
    }

    @Override
    protected void setLabelProvider(TableViewerColumn column) {
        containerCache.clear();

        column.setLabelProvider(new SearchCellLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return getWorkbenchPartReferenceText((WorkbenchPartReference) element);
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

            @Override
            public void update(@NotNull ViewerCell cell) {
                super.update(cell);

                if (!(cell.getElement() instanceof EditorReference ref)) {
                    return;
                }

                DBPDataSourceContainer container = resolveContainer(ref);
                if (container != null) {
                    ConnectionLabelUtils.applyConnectionInfo(cell, container);
                }
            }

            @Override
            public void dispose() {
                containerCache.clear();
                super.dispose();
            }
        });

        ColumnViewerToolTipSupport.enableFor(column.getViewer());
    }

    @Override
    protected void setMatcherString(String pattern) {
        this.pattern = pattern;
    }

    @NotNull
    private String getFilterText(@NotNull EditorReference ref) {
        String label = getWorkbenchPartReferenceText(ref);
        return ConnectionLabelUtils.appendConnectionSuffix(label, resolveContainer(ref));
    }

    @Nullable
    private DBPDataSourceContainer resolveContainer(@NotNull EditorReference ref) {
        if (containerCache.containsKey(ref)) {
            return containerCache.get(ref);
        }
        DBPDataSourceContainer container = extractDataSourceContainer(ref);
        containerCache.put(ref, container);
        return container;
    }

    @Nullable
    private static DBPDataSourceContainer extractDataSourceContainer(@NotNull EditorReference ref) {
        if (isResolving) {
            return null;
        }
        isResolving = true;
        try {
            // Use shared helper for the common IEditorPart → DBPDataSourceContainerProvider check
            DBPDataSourceContainer container = ConnectionLabelUtils.getDataSourceContainer(ref.getEditor(false));
            if (container != null) {
                return container;
            }

            // Editor not loaded; try editor input for lazy-loaded editors
            try {
                IEditorInput input = ref.getEditorInput();
                if (input instanceof DBPDataSourceContainerProvider provider) {
                    return provider.getDataSourceContainer();
                }
            } catch (Exception e) {
                log.debug("Cannot get editor input for: " + ref.getTitle(), e);
            }

            return null;
        } finally {
            isResolving = false;
        }
    }
}
