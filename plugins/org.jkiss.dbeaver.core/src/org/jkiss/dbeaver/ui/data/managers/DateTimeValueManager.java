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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.DateTimeInlineEditor;
import org.jkiss.dbeaver.ui.data.editors.DateTimeStandaloneEditor;

import java.util.Date;

/**
 * JDBC string value handler
 */
public class DateTimeValueManager extends BaseValueManager {

    protected static final Log log = Log.getLog(DateTimeValueManager.class);

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final IValueController controller, @Nullable IValueEditor activeEditor)
        throws DBCException
    {
        super.contributeActions(manager, controller, activeEditor);
        manager.add(new Action(CoreMessages.model_jdbc_set_to_current_time, DBeaverIcons.getImageDescriptor(DBIcon.TYPE_DATETIME)) {
            @Override
            public void run() {
                controller.updateValue(new Date());
            }
        });
    }

    @NotNull
    @Override
    public IValueController.EditType[] getSupportedEditTypes() {
        return new IValueController.EditType[] {IValueController.EditType.INLINE, IValueController.EditType.PANEL, IValueController.EditType.EDITOR};
    }

    @Override
    public IValueEditor createEditor(@NotNull IValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            case PANEL:
                return new DateTimeInlineEditor(controller);
            case EDITOR:
                return new DateTimeStandaloneEditor(controller);
            default:
                return null;
        }
    }

}