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
package org.jkiss.dbeaver.ui;

import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.commands.ICommandService;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.AbstractCommandContext;

/**
 * SimpleCommandContext.
 * Uses jface command service to update commands state
 */
public class SimpleCommandContext extends AbstractCommandContext {

    public SimpleCommandContext(DBCExecutionContext executionContext, boolean atomic) {
        super(executionContext, atomic);
    }

    protected void refreshCommandState()
    {
        ICommandService commandService = DBeaverUI.getActiveWorkbenchWindow().getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements(IWorkbenchCommandConstants.EDIT_UNDO, null);
            commandService.refreshElements(IWorkbenchCommandConstants.EDIT_REDO, null);
        }
    }

}