package org.jkiss.dbeaver.postgresql.internal.debug.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class PgSqlLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    public PgSqlLaunchConfigurationTabGroup()
    {
    }

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode)
    {
        PgSqlTab pgSqlTab = new PgSqlTab();
        CommonTab commonTab = new CommonTab();
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
                pgSqlTab,
                commonTab
            };
            setTabs(tabs);
    }

}
