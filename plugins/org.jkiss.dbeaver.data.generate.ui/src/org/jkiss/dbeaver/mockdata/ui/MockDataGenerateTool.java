// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.ui;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import java.util.Iterator;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import java.util.Collection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;

public class MockDataGenerateTool implements IUserInterfaceTool
{
    public void execute(final IWorkbenchWindow window, final IWorkbenchPart activePart, final Collection<DBSObject> objects) throws DBException {
        for (final DBSObject dbsObject : objects) {
            final DBPDataSource dataSource = dbsObject.getDataSource();
            if (dataSource.getInfo().isReadOnlyData()) {
                UIUtils.showMessageBox(UIUtils.getActiveWorkbenchShell(), "Read-only database", "Database '" + dataSource.getContainer().getName() + "' is read-only.\nMock data generation is not possible.", 8);
                return;
            }
        }
        final MockDataSettings mockDataSettings = new MockDataSettings(objects);
        final MockDataExecuteWizard wizard = new MockDataExecuteWizard(mockDataSettings);
        final TaskConfigurationWizardDialog dialog = new MockDataConfigurationWizardDialog(window, wizard, mockDataSettings);
        dialog.open();
    }
}
