package org.jkiss.dbeaver.internal.debug.ui;

import org.eclipse.debug.core.ILaunchManager;
import org.jkiss.dbeaver.debug.ui.LaunchContributionItem;

public class DebugContributionItem extends LaunchContributionItem {

    protected DebugContributionItem()
    {
        super(ILaunchManager.DEBUG_MODE);
    }

}
