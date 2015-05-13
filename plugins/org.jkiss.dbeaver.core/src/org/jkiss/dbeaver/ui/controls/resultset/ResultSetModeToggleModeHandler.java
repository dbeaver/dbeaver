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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import java.util.Map;

/**
 * ResultSetCommandHandler
 */
public class ResultSetModeToggleModeHandler extends ResultSetCommandHandler implements IElementUpdater {

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        if (element.getServiceLocator() instanceof IWorkbenchPartSite) {
            IWorkbenchPartSite partSite = (IWorkbenchPartSite) element.getServiceLocator();
            if (partSite.getPart() instanceof IResultSetContainer) {
                ResultSetViewer rsv = ((IResultSetContainer) partSite.getPart()).getResultSetViewer();
                if (rsv != null) {
                    if (!rsv.isRecordMode()) {
                        element.setText("Switch to record mode");
                        element.setChecked(true);
                    } else {
                        element.setText("Switch to grid mode");
                        element.setChecked(false);
                    }
                }
            }
        }
    }
}