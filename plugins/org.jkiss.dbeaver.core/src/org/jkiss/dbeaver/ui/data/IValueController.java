/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
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
     */
    void updateValue(@Nullable Object value);

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
    @Nullable
    Composite getEditPlaceholder();

    /**
     * Editor toolbar. Used with PANEL editors
     * @return toolbar or null if toolbar is not active
     */
    @Nullable
    IContributionManager getEditBar();

    /**
     * Closes current value editor.
     * This action may initiated by editor control (e.g. on Enter or Esc key)
     */
    void closeInlineEditor();

    /**
     * Closes current editor and activated next cell editor
     * @param next true for next and false for previous cell
     */
    void nextInlineEditor(boolean next);

    void unregisterEditor(IValueEditorStandalone editor);

    /**
     * Show error/warning message in grid control.
     * @param message error message
     * @param error true for error, false for informational message
     */
    void showMessage(String message, boolean error);

}
