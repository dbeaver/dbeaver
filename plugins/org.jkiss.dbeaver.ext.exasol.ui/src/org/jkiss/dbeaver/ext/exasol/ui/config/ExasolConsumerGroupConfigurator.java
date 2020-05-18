package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.exasol.model.ExasolConsumerGroup;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class ExasolConsumerGroupConfigurator implements DBEObjectConfigurator<ExasolConsumerGroup> {
    @Override
    public ExasolConsumerGroup configureObject(DBRProgressMonitor monitor, Object container, ExasolConsumerGroup group) {
        return new UITask<ExasolConsumerGroup>() {
            @Override
            protected ExasolConsumerGroup runTask() {
                ExasolConsumerGroupDialog dialog = new ExasolConsumerGroupDialog(UIUtils.getActiveWorkbenchShell(), group);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                group.setName(dialog.getName());
                group.setDescription(dialog.getComment());
                group.setCpuWeight(dialog.getCpuWeight());
                group.setSessionRamLimit(dialog.getSessionRamLimit());
                group.setUserRamLimit(dialog.getUserRamLimit());
                group.setGroupRamLimit(dialog.getGroupRamLimit());
                return group;
            }
        }.execute();
    }
}
