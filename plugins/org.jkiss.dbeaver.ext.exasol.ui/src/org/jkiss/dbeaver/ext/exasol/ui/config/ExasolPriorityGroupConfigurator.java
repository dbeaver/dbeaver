package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.exasol.model.ExasolPriorityGroup;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class ExasolPriorityGroupConfigurator implements DBEObjectConfigurator<ExasolPriorityGroup> {
    @Override
    public ExasolPriorityGroup configureObject(DBRProgressMonitor monitor, Object container, ExasolPriorityGroup group) {
        return new UITask<ExasolPriorityGroup>() {
            @Override
            protected ExasolPriorityGroup runTask() {
                ExasolPriorityGroupDialog dialog = new ExasolPriorityGroupDialog(UIUtils.getActiveWorkbenchShell(), group);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                group.setName(dialog.getName());
                group.setDescription(dialog.getComment());
                group.setWeight(dialog.getWeight());
                return group;
            }
        }.execute();
    }
}
