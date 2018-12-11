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
package org.jkiss.dbeaver.ext.mockdata.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.mockdata.MockDataGenerateTool;
import org.jkiss.dbeaver.ext.mockdata.MockDataMessages;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.ArrayList;
import java.util.List;

public class ResultSetMockDataHandler extends AbstractHandler {

    private static final Log log = Log.getLog(ResultSetMockDataHandler.class);

    public ResultSetMockDataHandler() {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        List<DBSObject> selectedObjects;
        IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet != null) {
            DBSDataContainer dataContainer = resultSet.getDataContainer();
            if (dataContainer == null || dataContainer.getDataSource() == null) {
                DBeaverUI.getInstance().showError(MockDataMessages.tools_mockdata_message_title, MockDataMessages.tools_mockdata_error_notconnected);
                return null;
            }
            if (!(dataContainer instanceof DBSDataManipulator)) {
                DBeaverUI.getInstance().showError(MockDataMessages.tools_mockdata_message_title, MockDataMessages.tools_mockdata_error_tableonly);
                return null;
            }
            selectedObjects = new ArrayList<>();
            selectedObjects.add(dataContainer);
        } else {
            selectedObjects = NavigatorUtils.getSelectedObjects(
                    HandlerUtil.getCurrentSelection(event));
        }

        MockDataGenerateTool mockDataGenerator = new MockDataGenerateTool();
        try {
            mockDataGenerator.execute(
                    HandlerUtil.getActiveWorkbenchWindow(event), null, selectedObjects);
        } catch (DBException e) {
            log.error("Error launching the Mock Data Generator", e);
        }

        return null;
    }
}
