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