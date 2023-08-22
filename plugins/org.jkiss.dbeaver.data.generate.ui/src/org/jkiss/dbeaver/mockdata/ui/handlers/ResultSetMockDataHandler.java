// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.ui.handlers;

import org.eclipse.core.commands.ExecutionException;
import java.util.List;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.DBException;
import java.util.Collection;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import java.util.ArrayList;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.core.commands.ExecutionEvent;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.mockdata.ui.MockDataGenerateTool;
import org.jkiss.dbeaver.mockdata.ui.internal.MockDataUIMessages;
import org.eclipse.core.commands.AbstractHandler;

public class ResultSetMockDataHandler extends AbstractHandler
{
    private static final Log log;
    
    static {
        log = Log.getLog(ResultSetMockDataHandler.class);
    }
    
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        final IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(HandlerUtil.getActivePart(event));
        List<DBSObject> selectedObjects;
        if (resultSet != null) {
            final DBSDataContainer dataContainer = resultSet.getDataContainer();
            if (dataContainer == null || dataContainer.getDataSource() == null) {
                DBWorkbench.getPlatformUI().showError(MockDataUIMessages.tools_mockdata_message_title, MockDataUIMessages.tools_mockdata_error_notconnected);
                return null;
            }
            if (!(dataContainer instanceof DBSDataManipulator)) {
                DBWorkbench.getPlatformUI().showError(MockDataUIMessages.tools_mockdata_message_title, MockDataUIMessages.tools_mockdata_error_tableonly);
                return null;
            }
            selectedObjects = new ArrayList<DBSObject>();
            selectedObjects.add((DBSObject)dataContainer);
        }
        else {
            selectedObjects = (List<DBSObject>)NavigatorUtils.getSelectedObjects(HandlerUtil.getCurrentSelection(event));
        }
        final MockDataGenerateTool mockDataGenerator = new MockDataGenerateTool();
        try {
            mockDataGenerator.execute(HandlerUtil.getActiveWorkbenchWindow(event), null, selectedObjects);
        }
        catch (DBException e) {
            ResultSetMockDataHandler.log.error((Object)"Error launching the Mock Data Generator", (Throwable)e);
        }
        return null;
    }
}
