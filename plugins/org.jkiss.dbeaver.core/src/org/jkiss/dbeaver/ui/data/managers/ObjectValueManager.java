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
package org.jkiss.dbeaver.ui.data.managers;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.dialogs.data.CursorViewDialog;

/**
 * Object value manager.
 */
public class ObjectValueManager extends StringValueManager {

    @Override
    public IValueEditor createEditor(@NotNull final IValueController controller)
        throws DBException
    {
        final Object value = controller.getValue();
        if (controller.getEditType() == IValueController.EditType.EDITOR) {
            if (value instanceof DBDCursor) {
                return new CursorViewDialog(controller);
            }
        }
        return super.createEditor(controller);
    }

}