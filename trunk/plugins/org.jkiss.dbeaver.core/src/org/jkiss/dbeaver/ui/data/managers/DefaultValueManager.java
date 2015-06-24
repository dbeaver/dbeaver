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
package org.jkiss.dbeaver.ui.data.managers;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPPropertyManager;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.editors.StringInlineEditor;
import org.jkiss.dbeaver.ui.dialogs.data.TextViewDialog;

/**
 * Default value handler
 */
public class DefaultValueManager implements IValueManager {

    public static final DefaultValueManager INSTANCE = new DefaultValueManager();


    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {
        // nothing
    }

    @Override
    public void contributeProperties(@NotNull DBPPropertyManager propertySource, @NotNull IValueController controller) {
        // nothing
    }

    @Override
    public IValueEditor createEditor(@NotNull final IValueController controller) throws DBException {
        switch (controller.getEditType()) {
            case INLINE:
            case PANEL:
                return new StringInlineEditor(controller);
            case EDITOR:
                return new TextViewDialog(controller);
            default:
                return null;
        }
    }

}
