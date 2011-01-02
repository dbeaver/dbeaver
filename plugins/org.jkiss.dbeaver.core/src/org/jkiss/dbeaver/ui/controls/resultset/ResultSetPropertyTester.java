/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
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
    public static final String PROP_CAN_MOVE = "canMove";
    public static final String PROP_EDITABLE = "editable";
    public static final String PROP_CHANGED = "changed";

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        Spreadsheet spreadsheet = Spreadsheet.getFromGrid((LightGrid) receiver);
        if (spreadsheet == null) {
            return false;
        }
        ResultSetViewer rsv = (ResultSetViewer)spreadsheet.getController();

        if (PROP_CAN_MOVE.equals(property)) {
            int currentRow = rsv.getCurrentRow();
            if ("back".equals(expectedValue)) {
                return currentRow > 0;
            } else if ("forward".equals(expectedValue)) {
                return currentRow < rsv.getRowsCount() - 1;
            }
        } else if (PROP_EDITABLE.equals(property)) {
            GridPos pos = rsv.getCurrentPosition();
            if ("edit".equals(expectedValue)) {
                return pos != null && rsv.isCellEditable(pos);
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
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
        service.requestEvaluation(NAMESPACE + "." + propName);
    }

}