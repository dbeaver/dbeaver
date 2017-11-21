package org.jkiss.dbeaver.postgresql.internal.debug.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class PgSqlLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    public PgSqlLaunchConfigurationTabGroup()
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode)
    {
        CommonTab commonTab = new CommonTab();
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
                commonTab
            };
            setTabs(tabs);
    }

}
