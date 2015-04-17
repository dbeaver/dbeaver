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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCommandHandler;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

import java.util.Map;

/**
 * ResultSetModeTogglePreviewHandler
 */
public class SpreadsheetTogglePreviewHandler extends SpreadsheetCommandHandler implements IElementUpdater {

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        Spreadsheet spreadsheet = getActiveSpreadsheet(event);
        if (spreadsheet == null) {
            return null;
        }

        spreadsheet.getPresentation().togglePreview();
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        if (element.getServiceLocator() instanceof IWorkbenchPartSite) {
            IWorkbenchPartSite partSite = (IWorkbenchPartSite) element.getServiceLocator();
            ResultSetViewer rsv = ResultSetCommandHandler.getActiveResultSet(partSite.getPart());
            if (rsv != null) {
                IResultSetPresentation presentation = rsv.getActivePresentation();
                if (presentation instanceof SpreadsheetPresentation && ((SpreadsheetPresentation) presentation).isPreviewVisible()) {
                    element.setText("Hide value view panel");
                    element.setChecked(true);
                } else {
                    element.setText("Show value view panel");
                    element.setChecked(false);
                }
            }
        }
    }
}