/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.widgets.Control;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

/**
 * DBD Value Editor.
 * Must be implemented by all visual value editors (dialogs, editors, inline controls).
 */
public interface DBDValueEditor
{
    /**
     * Create editor control(s)
     */
    void createControl();

    /**
     * Gets control which actually performs edit
     * @return control reference
     */
    Control getControl();

    /**
     * Extracts value from value editor.
     * @return value. Possibly NULL
     * @throws DBException on any error
     */
    @Nullable
    Object extractEditorValue()
        throws DBException;

    /**
     * Fills current editor with specified value. Do not updates value in controller.
     * @param value new value for editor
     * @throws DBException on any error
     */
    void primeEditorValue(@Nullable Object value)
        throws DBException;

}
