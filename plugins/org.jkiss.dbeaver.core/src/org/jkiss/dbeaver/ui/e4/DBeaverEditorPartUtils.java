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

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
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
import org.jkiss.dbeaver.ui.editors.EditorUtils;

import java.util.WeakHashMap;

public final class DBeaverEditorPartUtils {
    private static final Log log = Log.getLog(DBeaverEditorPartUtils.class);

    private static final String PART_SKIP_KEY = DBeaverEditorPartUtils.class.getName() + ".skipPart";

    // Avoids UI double entrance (e.g. master password dialog) when resolving editor input. SWT UI thread is single-threaded, so volatile is not needed.
    private static boolean isResolving;

    // Weak so that editor refs/parts can be GCed when no longer used. Caches container lookups for Ctrl+E popup and chevron list.
    private static final WeakHashMap<Object, DBPDataSourceContainer> cache = new WeakHashMap<>();

    private DBeaverEditorPartUtils() {
    }

    /**
     * Extracts a {@link DBPDataSourceContainer} from an editor reference.
     * Uses a shared reentrancy guard and cache so that Ctrl+E and chevron list can call without duplicating logic.
     */
    @Nullable
    public static DBPDataSourceContainer getDataSourceContainer(@NotNull IEditorReference ref) {
        if (cache.containsKey(ref)) {
            return cache.get(ref);
        }
        if (isResolving) {
            return null;
        }
        isResolving = true;
        try {
            DBPDataSourceContainer container;
            try {
                container = getDataSourceContainerFromRef(ref);
            } catch (Exception e) {
                log.debug("Cannot get editor input for: " + ref.getTitle(), e);
                container = null;
            }
            cache.put(ref, container);
            return container;
        } finally {
            isResolving = false;
        }
    }

    @Nullable
    static DBPDataSourceContainer getDataSourceContainer(@NotNull MPart part) {
        if (part.getTransientData().containsKey(PART_SKIP_KEY)) {
            return null;
        }
        if (cache.containsKey(part)) {
            return cache.get(part);
        }
        if (isResolving) {
            return null;
        }
        isResolving = true;
        try {
            DBPDataSourceContainer container = getDataSourceContainerFromPart(part);
            cache.put(part, container);
            return container;
        } finally {
            isResolving = false;
        }
    }

    @Nullable
    private static DBPDataSourceContainer getDataSourceContainerFromRef(@NotNull IEditorReference ref) throws Exception {
        DBPDataSourceContainer container = getDataSourceContainer(ref.getEditor(false));
        if (container != null) {
            return container;
        }
        IEditorInput input = ref.getEditorInput();
        if (input instanceof DBPDataSourceContainerProvider provider) {
            return provider.getDataSourceContainer();
        }
        return null;
    }

    @Nullable
    private static DBPDataSourceContainer getDataSourceContainerFromPart(@NotNull MPart part) {
        if (part.getObject() instanceof CompatibilityEditor editor) {
            return getDataSourceContainer(editor.getEditor());
        }

        if (part.getTransientData().get(IWorkbenchPartReference.class.getName()) instanceof IEditorReference ref) {
            IEditorPart editor = ref.getEditor(false);
            if (editor != null) {
                return getDataSourceContainer(editor);
            }
            try {
                return EditorUtils.getInputDataSource(ref.getEditorInput(), false);
            } catch (Exception e) {
                part.getTransientData().put(PART_SKIP_KEY, Boolean.TRUE);
                log.debug("Cannot get editor input for part: " + part.getElementId(), e);
            }
        }
        return null;
    }

    @Nullable
    private static DBPDataSourceContainer getDataSourceContainer(@Nullable IEditorPart editorPart) {
        if (editorPart instanceof DBPDataSourceContainerProvider provider) {
            DBPDataSourceContainer container = provider.getDataSourceContainer();
            if (container != null) {
                return container;
            }
        }
        if (editorPart != null) {
            IEditorInput input = editorPart.getEditorInput();
            if (input instanceof DBPDataSourceContainerProvider provider) {
                return provider.getDataSourceContainer();
            }
        }
        if (editorPart != null) {
            return EditorUtils.getInputDataSource(editorPart.getEditorInput(), false);
        }
        return null;
    }
}
