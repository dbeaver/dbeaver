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

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * DatabaseEditorPropertyTester
 */
public class ResultSetPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.core.resultset";
    public static final String PROP_ACTIVE = "active";
    public static final String PROP_HAS_DATA = "hasData";
    public static final String PROP_CAN_COPY = "canCopy";
    public static final String PROP_CAN_PASTE = "canPaste";
    public static final String PROP_CAN_CUT = "canCut";
    public static final String PROP_CAN_MOVE = "canMove";
    public static final String PROP_CAN_TOGGLE = "canToggle";
    public static final String PROP_CAN_SWITCH_PRESENTATION = "canSwitchPresentation";
    public static final String PROP_EDITABLE = "editable";
    public static final String PROP_CHANGED = "changed";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        ResultSetViewer rsv = ResultSetCommandHandler.getActiveResultSet((IWorkbenchPart)receiver);
        return rsv != null && checkResultSetProperty(rsv, property, expectedValue);
    }

    private boolean checkResultSetProperty(ResultSetViewer rsv, String property, Object expectedValue)
    {
        if (PROP_ACTIVE.equals(property)) {
            return true;
        } else if (PROP_HAS_DATA.equals(property)) {
            return rsv.getModel().getRowCount() > 0;
        } else if (PROP_CAN_COPY.equals(property)) {
            return rsv.getActivePresentation().getCurrentAttribute() != null && rsv.getCurrentRow() != null;
        } else if (PROP_CAN_PASTE.equals(property) || PROP_CAN_CUT.equals(property)) {
            DBDAttributeBinding attr = rsv.getActivePresentation().getCurrentAttribute();
            return attr != null && !rsv.isAttributeReadOnly(attr);
        } else if (PROP_CAN_MOVE.equals(property)) {
            ResultSetRow currentRow = rsv.getCurrentRow();
            if ("back".equals(expectedValue)) {
                return currentRow != null && currentRow.getVisualNumber() > 0;
            } else if ("forward".equals(expectedValue)) {
                return currentRow != null && currentRow.getVisualNumber() < rsv.getModel().getRowCount() - 1;
            }
        } else if (PROP_EDITABLE.equals(property)) {
            if (!rsv.hasData()) {
                return false;
            }
            if ("edit".equals(expectedValue) || "inline".equals(expectedValue)) {
                DBDAttributeBinding attr = rsv.getActivePresentation().getCurrentAttribute();
                if (attr == null) {
                    return false;
                }
                if ("inline".equals(expectedValue)) {
                    return !rsv.isAttributeReadOnly(attr);
                } else {
                    return true;
                }
            } else if ("add".equals(expectedValue)) {
                return rsv.isInsertable();
            } else if ("copy".equals(expectedValue) || "delete".equals(expectedValue)) {
                ResultSetRow currentRow = rsv.getCurrentRow();
                return currentRow != null && rsv.isInsertable();
            } else {
                return false;
            }
        } else if (PROP_CHANGED.equals(property)) {
            return rsv.isDirty();
        } else if (PROP_CAN_TOGGLE.equals(property)) {
            return rsv.getActivePresentation().getControl().isFocusControl();
        } else if (PROP_CAN_SWITCH_PRESENTATION.equals(property)) {
            return !rsv.isRefreshInProgress() && rsv.getAvailablePresentations() != null && rsv.getAvailablePresentations().size() > 1;
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