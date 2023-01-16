/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;

import java.util.HashSet;

/**
 * Spreadsheet command handler.
 * Active when focus is in spreadsheet control
 */
public class SpreadsheetCommandHandler extends AbstractHandler {

    public static final String CMD_COLUMNS_FIT_VALUE = "org.jkiss.dbeaver.core.resultset.grid.columnsFitValue";
    public static final String CMD_COLUMNS_FIT_SCREEN = "org.jkiss.dbeaver.core.resultset.grid.columnsFitScreen";
    public static final String CMD_COLUMNS_HIDE_EMPTY = "org.jkiss.dbeaver.core.resultset.grid.columnsHideEmpty";

    public static SpreadsheetPresentation getActiveSpreadsheet(ExecutionEvent event)
    {
        IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet != null) {
            IResultSetPresentation presentation = resultSet.getActivePresentation();
            if (presentation instanceof SpreadsheetPresentation) {
                return (SpreadsheetPresentation) presentation;
            }
        }

        return null;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        SpreadsheetPresentation spreadsheet = getActiveSpreadsheet(event);
        if (spreadsheet == null) {
            return null;
        }

        String actionId = event.getCommand().getId();
        switch (actionId) {
            case CMD_COLUMNS_FIT_VALUE:
                spreadsheet.getSpreadsheet().packColumns(true);
                break;
            case CMD_COLUMNS_FIT_SCREEN:
                spreadsheet.getSpreadsheet().packColumns(false);
                break;
            case CMD_COLUMNS_HIDE_EMPTY: {
                final var model = spreadsheet.getController().getModel();
                final var attributes = new HashSet<>(model.getVisibleAttributes());
                for (ResultSetRow row : model.getAllRows()) {
                    attributes.removeIf(attribute -> !DBUtils.isNullValue(model.getCellValue(attribute, row)));
                }
                if (!attributes.isEmpty()) {
                    for (DBDAttributeBinding attribute : attributes) {
                        model.setAttributeVisibility(attribute, false);
                    }
                    spreadsheet.refreshData(true, false, true);
                }
                break;
            }
        }

        return null;
    }
}
