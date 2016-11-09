/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
