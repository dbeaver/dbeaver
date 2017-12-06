package org.jkiss.dbeaver.debug.internal.ui;

import org.eclipse.debug.core.ILaunchManager;
import org.jkiss.dbeaver.debug.ui.LaunchContributionItem;

public class DebugContributionItem extends LaunchContributionItem {

    protected DebugContributionItem()
    {
        super(ILaunchManager.DEBUG_MODE);
    }

}
