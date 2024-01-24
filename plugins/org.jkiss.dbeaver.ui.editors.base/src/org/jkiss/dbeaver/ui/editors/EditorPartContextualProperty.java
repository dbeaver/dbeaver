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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class EditorPartContextualProperty {

    private static final Log log = Log.getLog(EditorPartContextualProperty.class);
    
    private static final Map<String, EditorPartContextualProperty> knownProps = Collections.synchronizedMap(new HashMap<>());
    
    public final String partPropName;
    public final QualifiedName filePropName;
    public final String globalPrefName;
    public final String defaultValue;
    
    public static class PartCustomPropertyValueInfo {
        public final String value;
        public final boolean isInitial;

        public PartCustomPropertyValueInfo(@NotNull String value, boolean isInitial) {
            this.value = value;
            this.isInitial = isInitial;
        }
    }
    
    private EditorPartContextualProperty(
        @NotNull String partPropName,
        @NotNull QualifiedName filePropName,
        @NotNull String globalPrefName,
        @NotNull String defaultValue
    ) {
        this.partPropName = partPropName;
        this.filePropName = filePropName;
        this.globalPrefName = globalPrefName;
        this.defaultValue = defaultValue;
    }
    
    public static EditorPartContextualProperty setup(
        @NotNull String partPropName,
        @NotNull QualifiedName filePropName,
        @NotNull String globalPrefName,
        @NotNull String defaultValue
    ) {
        return knownProps.computeIfAbsent(
            partPropName,
            partPropName2 -> new EditorPartContextualProperty(partPropName2, filePropName, globalPrefName, defaultValue)
        );
    }
    
    /**
     * Get SQL Editor property value
     */
    @NotNull
    public PartCustomPropertyValueInfo getPropertyValue(@NotNull EditorPart editor) {
        String value = editor.getPartProperty(partPropName);
        boolean isInitial;
        if (value == null) {
            value = getPropertyValueInitial(editor);
            isInitial = true;
        } else {
            isInitial = false;
        }
        return new PartCustomPropertyValueInfo(value, isInitial);
    }

    @NotNull
    private String getPropertyValueInitial(@NotNull EditorPart editor) {
        IFile activeFile = EditorUtils.getFileFromInput(editor.getEditorInput());
        if (activeFile != null && activeFile.exists()) {
            try {
                String value = activeFile.getPersistentProperty(filePropName);
                if (value != null) {
                    return value;
                }
            } catch (CoreException e) {
                log.debug(e.getMessage(), e);
            }
        }

        final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        return store.contains(globalPrefName) ? store.getString(globalPrefName) : defaultValue;
    }

    /**
     * Set SQL Editor property value
     */
    public boolean setPropertyValue(@NotNull EditorPart editor, @Nullable String value) {
        boolean changed;
        String oldValue = editor.getPartProperty(partPropName);
        if (value == null) {
            editor.setPartProperty(partPropName, defaultValue);
            changed = true;
        } else {
            if (CommonUtils.equalObjects(oldValue, value)) {
                changed = false;
            } else {
                editor.setPartProperty(partPropName, value);
                IFile activeFile = EditorUtils.getFileFromInput(editor.getEditorInput());
                if (activeFile != null) {
                    try {
                        activeFile.setPersistentProperty(filePropName, value);
                    } catch (CoreException e) {
                        log.debug(e.getMessage(), e);
                    }
                }
                changed = true;
            }
        }
        if (changed) {
            DBWorkbench.getPlatform().getPreferenceStore().firePropertyChangeEvent(globalPrefName, oldValue, value);
        }
        return changed;
    }
}
