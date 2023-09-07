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
package org.jkiss.dbeaver.ui.controls.resultset.colors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCellLocation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.SpreadsheetPresentation;

public class SetRowColorHandler extends AbstractHandler {

    public SetRowColorHandler() {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SpreadsheetPresentation spreadsheet = getActiveSpreadsheet(event);
        if (spreadsheet == null) {
            return null;
        }
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart != null) {
            ResultSetViewer resultSetViewer = activePart.getAdapter(ResultSetViewer.class);

            RGB color;
            final Shell shell = UIUtils.createCenteredShell(resultSetViewer.getControl().getShell());
            try {
                ColorDialog cd = new ColorDialog(shell);
                color = cd.open();
                if (color == null) {
                    return null;
                }
            } finally {
                UIUtils.disposeCenteredShell(shell);
            }
            try {
                final DBVEntity vEntity = getColorsVirtualEntity(resultSetViewer);
                ResultSetCellLocation currentCellLocation = spreadsheet.getCurrentCellLocation();
                Object cellValue = resultSetViewer.getContainer().getResultSetController().getModel()
                        .getCellValue(currentCellLocation);

             
                vEntity.setColorOverride(spreadsheet.getFocusAttribute(), cellValue , null,
                        StringConverter.asString(color));
                
                updateColors(resultSetViewer, vEntity, true);
            } catch (IllegalStateException e) {
                DBWorkbench.getPlatformUI().showError("Row color", "Can't set row color", e);
            }
        }
        return null;
    }
    
    SpreadsheetPresentation getActiveSpreadsheet(ExecutionEvent event) {
        IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet != null) {
            IResultSetPresentation presentation = resultSet.getActivePresentation();
            if (presentation instanceof SpreadsheetPresentation) {
                return (SpreadsheetPresentation) presentation;
            }
        }
        return null;
    }
    
    void updateColors(ResultSetViewer resultSetViewer, DBVEntity entity, boolean refresh) {
        resultSetViewer.getModel().updateColorMapping(true);
        entity.persistConfiguration();
        if (refresh) {
            resultSetViewer.redrawData(false, false);
        }
    }
    @NotNull
    DBVEntity getColorsVirtualEntity(ResultSetViewer resultSetViewer) throws IllegalStateException {
        DBSDataContainer dataContainer = resultSetViewer.getDataContainer();
        if (dataContainer == null) {
            throw new IllegalStateException("No data container");
        }
        return DBVUtils.getVirtualEntity(dataContainer, true);
    }

    
}
