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
    public static final String PROP_HAS_MORE_DATA = "hasMoreData";
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
        boolean actionsDisabled = rsv.isActionsDisabled();

        switch (property) {
            case PROP_ACTIVE:
                return true;
            case PROP_HAS_DATA:
                return rsv.getModel().hasData();
            case PROP_HAS_MORE_DATA:
                return rsv.isHasMoreData();
            case PROP_CAN_COPY:
                return !actionsDisabled && rsv.getModel().hasData();
            case PROP_CAN_PASTE:
            case PROP_CAN_CUT: {
                DBDAttributeBinding attr = rsv.getActivePresentation().getCurrentAttribute();
                return !actionsDisabled && attr != null && !rsv.isAttributeReadOnly(attr);
            }
            case PROP_CAN_MOVE: {
                if (actionsDisabled) return false;
                ResultSetRow currentRow = rsv.getCurrentRow();
                if ("back".equals(expectedValue)) {
                    return currentRow != null && currentRow.getVisualNumber() > 0;
                } else if ("forward".equals(expectedValue)) {
                    return currentRow != null && currentRow.getVisualNumber() < rsv.getModel().getRowCount() - 1;
                }
                break;
            }
            case PROP_EDITABLE: {
                if (actionsDisabled || !rsv.hasData()) {
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
            }
            case PROP_CHANGED:
                return rsv.isDirty();
            case PROP_CAN_TOGGLE:
                return
                    !actionsDisabled &&
                        rsv.getActivePresentation().getControl().isFocusControl();
            case PROP_CAN_SWITCH_PRESENTATION:
                return
                    !actionsDisabled &&
                        !rsv.isRefreshInProgress() &&
                        rsv.getAvailablePresentations() != null &&
                        rsv.getAvailablePresentations().size() > 1;
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}