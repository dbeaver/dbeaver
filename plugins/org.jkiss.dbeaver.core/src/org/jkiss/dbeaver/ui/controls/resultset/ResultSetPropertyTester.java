/*
 * Copyright (C) 2010-2012 Serge Rieder
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;
import org.jkiss.dbeaver.ui.controls.spreadsheet.Spreadsheet;

/**
 * DatabaseEditorPropertyTester
 */
public class ResultSetPropertyTester extends PropertyTester
{
    static final Log log = LogFactory.getLog(ResultSetPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.resultset";
    public static final String PROP_HAS_DATA = "hasData";
    public static final String PROP_CAN_COPY = "canCopy";
    //public static final String PROP_CAN_COPY_SPECIAL = "canCopySpecial";
    public static final String PROP_CAN_PASTE = "canPaste";
    public static final String PROP_CAN_CUT = "canCut";
    public static final String PROP_CAN_MOVE = "canMove";
    public static final String PROP_CAN_TOGGLE = "canToggle";
    public static final String PROP_EDITABLE = "editable";
    public static final String PROP_CHANGED = "changed";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        Spreadsheet spreadsheet = Spreadsheet.getFromGrid((LightGrid) receiver);
        if (spreadsheet == null) {
            return false;
        }
        ResultSetViewer rsv = (ResultSetViewer)spreadsheet.getController();
        if (rsv != null) {
            return checkResultSetProperty(rsv, property, expectedValue);
        } else {
            return false;
        }
    }

    private boolean checkResultSetProperty(ResultSetViewer rsv, String property, Object expectedValue)
    {
        if (PROP_HAS_DATA.equals(property)) {
            return rsv.getRowsCount() > 0;
        } else if (PROP_CAN_COPY.equals(property)) {
            final GridPos currentPosition = rsv.getCurrentPosition();
            return rsv.isValidCell(currentPosition);
        } else if (PROP_CAN_PASTE.equals(property) || PROP_CAN_CUT.equals(property)) {
            final GridPos currentPosition = rsv.getCurrentPosition();
            return rsv.isValidCell(currentPosition) && !rsv.isColumnReadOnly(currentPosition);
        } else if (PROP_CAN_MOVE.equals(property)) {
            int currentRow = rsv.getCurrentRow();
            if ("back".equals(expectedValue)) {
                return currentRow > 0;
            } else if ("forward".equals(expectedValue)) {
                return currentRow < rsv.getRowsCount() - 1;
            }
        } else if (PROP_EDITABLE.equals(property)) {
            if ("edit".equals(expectedValue)) {
                GridPos pos = rsv.getCurrentPosition();
                return pos != null && rsv.isValidCell(pos);
            } else if ("inline".equals(expectedValue)) {
                GridPos pos = rsv.getCurrentPosition();
                return pos != null && !rsv.isColumnReadOnly(pos);
            } else if ("add".equals(expectedValue) || "copy".equals(expectedValue)) {
                return rsv.isInsertable();
            } else if ("delete".equals(expectedValue)) {
                int currentRow = rsv.getCurrentRow();
                return currentRow >= 0 && currentRow < rsv.getRowsCount() && rsv.isInsertable();
            } else {
                return false;
            }
        } else if (PROP_CHANGED.equals(property)) {
            return rsv.hasChanges();
        } else if (PROP_CAN_TOGGLE.equals(property)) {
            return true;
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
//        ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
//        if (commandService != null) {
//            commandService.refreshElements(NAMESPACE + "." + propName, null);
//            System.out.println("REFRESH " + NAMESPACE + "." + propName);
//        }
    }

}