/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.utils.CommonUtils;

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
    public static final String PROP_CAN_NAVIGATE_LINK = "canNavigateLink";
    public static final String PROP_SUPPORTS_COUNT = "supportsCount";
    public static final String PROP_CAN_NAVIGATE_HISTORY = "canNavigateHistory";
    public static final String PROP_EDITABLE = "editable";
    public static final String PROP_CHANGED = "changed";

    private static final Log log = Log.getLog(ResultSetPropertyTester.class);

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        try {
            ResultSetViewer rsv = (ResultSetViewer) ResultSetHandlerMain.getActiveResultSet((IWorkbenchPart)receiver);
            return rsv != null && checkResultSetProperty(rsv, property, expectedValue);
        } catch (Throwable e) {
            if (!DBWorkbench.getPlatform().isShuttingDown()) {
                // FIXME: bug in Eclipse. To remove in future.
                log.debug(e);
            }
            return false;
        }
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
                return !actionsDisabled && rsv.isPresentationInFocus();
            case PROP_CAN_SWITCH_PRESENTATION:
                return
                    !actionsDisabled &&
                        !rsv.isRefreshInProgress() &&
                        rsv.getAvailablePresentations() != null &&
                        rsv.getAvailablePresentations().size() > 1;
            case PROP_SUPPORTS_COUNT:
                return rsv.hasData() && rsv.isHasMoreData() &&
                    (rsv.getDataContainer().getSupportedFeatures() & DBSDataContainer.DATA_COUNT) != 0;
            case PROP_CAN_NAVIGATE_LINK:
                if (!actionsDisabled && rsv.getModel().hasData()) {
                    final ResultSetRow row = rsv.getCurrentRow();
                    if (row != null) {
                        DBDAttributeBinding attr = rsv.getActivePresentation().getCurrentAttribute();
                        if (attr != null) {
                            Object value = rsv.getModel().getCellValue(attr, row);
                            return !CommonUtils.isEmpty(attr.getReferrers()) && !DBUtils.isNullValue(value);
                        }
                    }
                }
                return false;
            case PROP_CAN_NAVIGATE_HISTORY:
                if (!actionsDisabled && rsv.getModel().hasData()) {
                    if (expectedValue instanceof Number && ((Number)expectedValue).intValue() == 1 || "1".equals(expectedValue)) {
                        return rsv.getHistoryPosition() < rsv.getHistorySize() - 1;
                    } else {
                        return rsv.getHistoryPosition() > 0;
                    }
                }
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}