/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * DBD Value Controller
 */
public interface IValueController
{
    /**
     * Value editor type
     */
    enum EditType {
        NONE,   // Void editor, should be ignored by users
        INLINE, // Inline editor, appears right in grid's cell
        PANEL,  // "Preview" editor, appears on a special grid panel.
                // May be reused to edit different cells of the same type.
        EDITOR  // Separate editor, dialog or standalone editor window
    }

    /**
     * Active execution context. Context lifetime is longer than value handler lifetime.
     * @return execution context
     */
    @NotNull
    DBCExecutionContext getExecutionContext();

    /**
     * Value name (name of attribute or other metadata object)
     * @return value name
     */
    String getValueName();

    /**
     * Value type
     * @return meta data
     */
    DBSTypedObject getValueType();

    /**
     * Column value
     * @return value
     */
    @Nullable
    Object getValue();

    /**
     * Updates value
     * @param value value
     * @param updatePresentation    refresh UI
     */
    void updateValue(@Nullable Object value, boolean updatePresentation);

    /**
     * Updates value in all selected cells
     * @param value value
     */
    void updateSelectionValue(@Nullable Object value);

    /**
     * Associated value handler
     * @return value handler
     */
    DBDValueHandler getValueHandler();

    /**
     * Associated value manager
     * @return value manager
     */
    IValueManager getValueManager();

    EditType getEditType();

    /**
     * True if current cell is read-only.
     * @return read only flag
     */
    boolean isReadOnly();

    /**
     * Controller's host site
     * @return site
     */
    IWorkbenchPartSite getValueSite();

    /**
     * Inline or panel placeholder. Editor controls should be created inside of this placeholder.
     * In case of separated editor it is null.
     * @return placeholder control or null
     */
    Composite getEditPlaceholder();

    /**
     * Refreshes (recreates) editor.
     */
    void refreshEditor();

    /**
     * Show error/warning message in grid control.
     * @param messageType status message type
     * @param message error message
     */
    void showMessage(String message, DBPMessageType messageType);

}
