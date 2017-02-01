/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCommandHandler;

/**
 * Spreadsheet command handler.
 * Active when focus is in spreadsheet control
 */
public abstract class SpreadsheetCommandHandler extends AbstractHandler {

    public static Spreadsheet getActiveSpreadsheet(ExecutionEvent event)
    {
        Object control = HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
        if (control instanceof Spreadsheet) {
            return (Spreadsheet)control;
        }

        IResultSetController rsv = ResultSetCommandHandler.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (rsv != null) {
            IResultSetPresentation activePresentation = rsv.getActivePresentation();
            if (activePresentation instanceof SpreadsheetPresentation) {
                return ((SpreadsheetPresentation) activePresentation).getSpreadsheet();
            }
        }

        return null;
    }

}
