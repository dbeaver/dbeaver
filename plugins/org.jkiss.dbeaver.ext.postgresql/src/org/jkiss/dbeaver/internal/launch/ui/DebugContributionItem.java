package org.jkiss.dbeaver.internal.launch.ui;

import org.eclipse.debug.core.ILaunchManager;
import org.jkiss.dbeaver.launch.ui.LaunchContributionItem;

public class DebugContributionItem extends LaunchContributionItem {

    protected DebugContributionItem()
    {
        super(ILaunchManager.DEBUG_MODE);
    }

}
