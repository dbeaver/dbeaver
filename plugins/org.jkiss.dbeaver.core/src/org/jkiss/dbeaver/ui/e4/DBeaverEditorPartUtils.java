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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.ConnectionLabelUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;

final class DBeaverEditorPartUtils {
    private static final Log log = Log.getLog(DBeaverEditorPartUtils.class);

    private static final String PART_SKIP_KEY = DBeaverEditorPartUtils.class.getName() + ".skipPart";

    // We fix it by avoiding UI double entrance. SWT UI thread is single-threaded,
    // so volatile is not needed here.
    private static boolean isResolving;

    private DBeaverEditorPartUtils() {
    }

    @Nullable
    static DBPDataSourceContainer getDataSourceContainer(@NotNull MPart part) {
        if (part.getTransientData().containsKey(PART_SKIP_KEY)) {
            return null;
        }
        if (isResolving) {
            return null;
        }
        isResolving = true;
        try {
            if (part.getObject() instanceof CompatibilityEditor editor) {
                return getDataSourceContainer(editor.getEditor());
            }

            // See org.eclipse.ui.internal.WorkbenchPartReference.WorkbenchPartReference
            if (part.getTransientData().get(IWorkbenchPartReference.class.getName()) instanceof IEditorReference ref) {
                IEditorPart editor = ref.getEditor(false);
                if (editor != null) {
                    return getDataSourceContainer(editor);
                }

                try {
                    return EditorUtils.getInputDataSource(ref.getEditorInput(), false);
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
            isResolving = false;
        }
    }

    @Nullable
    private static DBPDataSourceContainer getDataSourceContainer(@NotNull IEditorPart editorPart) {
        DBPDataSourceContainer container = ConnectionLabelUtils.getDataSourceContainer(editorPart);
        if (container != null) {
            return container;
        }
        // Additional fallback for file-based editors (e.g. SQL scripts with connection stored as file property)
        return EditorUtils.getInputDataSource(editorPart.getEditorInput(), false);
    }
}
